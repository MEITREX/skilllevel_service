package de.unistuttgart.iste.gits.skilllevel_service.service;

import de.unistuttgart.iste.gits.generated.dto.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientResponseField;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Client for the content service, allowing to query contents with user progress data.
 */
@Component
@Slf4j
public class ContentServiceClient {

    private static final long RETRY_COUNT = 3;

    @Value("${content_service.url}")
    private String contentServiceUrl;

    private final ModelMapper modelMapper;

    public ContentServiceClient() {
        modelMapper = new ModelMapper();
        modelMapper.typeMap(String.class, OffsetDateTime.class).setConverter(context -> {
            String source = context.getSource();
            if(source == null) {
                return null;
            }
            return OffsetDateTime.parse(source);
        });
    }

    public UUID getChapterIdOfContent(UUID contentId) {
        try {
            WebClient webClient = WebClient.builder().baseUrl(contentServiceUrl).build();

            GraphQlClient client = HttpGraphQlClient.builder(webClient).build();

            String query = """
                    query($contentId: UUID!) {
                        contentsByIds(ids: [$contentId]) {
                            metadata {
                                chapterId
                            }
                        }
                    }
                    """;

            return client.document(query)
                    .variable("contentId", contentId)
                    .retrieve("contentsByIds[0].metadata.chapterId").toEntity(UUID.class)
                    .retry(RETRY_COUNT)
                    .block();

        } catch (Exception e) {
            throw new ContentServiceConnectionException("Error while fetching contents from content service", e);
        }
    }

    /**
     * Calls the content service to get the contents of a particular chapter. Note that this method only returns
     * generic content metadata, not data of the specific types like flashcard sets or quizzes.
     *
     * @param userId     the user id
     * @param chapterId the chapter id
     * @return the list of contents
     */
    public List<Content> getContentsOfChapter(UUID userId, UUID chapterId) {
        try {
            WebClient webClient = WebClient.builder().baseUrl(contentServiceUrl).build();

            GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

            String query = """
                    query($userId: UUID!, $chapterIds: [UUID!]!) {
                        contentsByChapterIds(chapterIds: $chapterIds) {
                            id
                            metadata {
                                name
                                tagNames
                                suggestedDate
                                type
                                chapterId
                                rewardPoints
                            }
                            progressDataForUser(userId: $userId) {
                                userId
                                contentId
                                learningInterval
                                nextLearnDate
                                lastLearnDate
                                log {
                                    timestamp
                                    success
                                    correctness
                                    hintsUsed
                                    timeToComplete
                                }
                            }
                            ...on Assessment {
                                assessmentMetadata {
                                    skillTypes
                                    skillPoints
                                }
                            }
                        }
                    }
                                    
                    """;

            log.info("Sending contentsByChapterIds query to course service with chapterId " + chapterId);

            // we must use media content here because the content type is an interface
            // that cannot be used for deserialization
            List<Content> result = graphQlClient.document(query)
                    .variable("userId", userId)
                    .variable("chapterIds", List.of(chapterId))
                    .execute()
                    .<List<Content>>handle((res, sink) -> {
                        if(!res.isValid()) {
                            sink.error(new ContentServiceConnectionException(
                                    "Error while fetching contents from content service: Invalid response.",
                                    res.getErrors()));
                            return;
                        }

                        List<Map<String, Object>> contentFields = res.field("contentsByChapterIds[0]").getValue();

                        if(contentFields == null) {
                            sink.error(new ContentServiceConnectionException(
                                    "Error while fetching contents from content service: Missing field in response."));
                            return;
                        }

                        List<Content> retrievedContents = new ArrayList<>();

                        for(Map<String, Object> contentField : contentFields) {
                            ContentMetadata metadata = modelMapper.map(contentField.get("metadata"), ContentMetadata.class);
                            UUID id = UUID.fromString((String)contentField.get("id"));
                            UserProgressData progressDataForUser = modelMapper.map(contentField.get("progressDataForUser"), UserProgressData.class);

                            if(contentField.containsKey("assessmentMetadata")) {
                                AssessmentMetadata assessmentMetadata = modelMapper.map(contentField.get("assessmentMetadata"), AssessmentMetadata.class);
                                retrievedContents.add(new GenericAssessmentResponse(id, metadata, assessmentMetadata, progressDataForUser));
                            } else {
                                retrievedContents.add(new ContentResponse(id, metadata, progressDataForUser));
                            }
                        }

                        sink.next(retrievedContents);
                    })
                    .retry(RETRY_COUNT)
                    .block();

            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            throw new ContentServiceConnectionException("Error while fetching contents from content service", e);
        }
    }

    @Getter
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class GenericAssessmentResponse extends ContentResponse implements Assessment {
        private AssessmentMetadata assessmentMetadata;

        public GenericAssessmentResponse(UUID id,
                                         ContentMetadata metadata,
                                         AssessmentMetadata assessmentMetadata,
                                         UserProgressData progressDataForUser) {
            super(id, metadata, progressDataForUser);
            this.assessmentMetadata = assessmentMetadata;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class ContentResponse implements Content {
        private UUID id;
        private ContentMetadata metadata;
        private UserProgressData progressDataForUser;

        @Override
        public @NotNull UserProgressData getUserProgressData() {
            return progressDataForUser;
        }
    }

    public static class ContentServiceConnectionException extends RuntimeException {

        private final String message;

        public ContentServiceConnectionException(String message, Exception e) {
            super(e);
            this.message = message;
        }

        public ContentServiceConnectionException(String message, List<ResponseError> errors) {
            super();
            StringBuilder sb = new StringBuilder(message);
            sb.append("GraphQl Response Errors: \n");
            for (ResponseError error : errors) {
                sb.append(error.getMessage())
                        .append(" at path ").append(error.getPath())
                        .append("\n");
            }

            this.message = sb.toString();
        }

        public ContentServiceConnectionException(String message) {
            super();
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
