package de.unistuttgart.iste.gits.skilllevel_service.service;

import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    public UUID getChapterIdOfContent(UUID contentId) {
        try {
            WebClient webClient = WebClient.builder().baseUrl(contentServiceUrl).build();

            GraphQlClient client = HttpGraphQlClient.builder(webClient).build();

            String query = """
                    query($contentId: UUID!) {
                        contentsByIds(ids: [$contentId]) {
                            chapterId
                        }
                    }
                    """;

            return client.document(query)
                    .variable("contentId", contentId)
                    .retrieve("contentsByIds[0].chapterId").toEntity(UUID.class)
                    .retry(RETRY_COUNT)
                    .block();

        } catch (Exception e) {
            throw new ContentServiceConnectionException("Error while fetching contents from content service", e);
        }
    }

    /**
     * Calls the content service to get the assessments of a particular chapter. Note that this method only returns
     * generic assessment objects, not the specific types like flashcard sets or quizzes.
     *
     * @param userId     the user id
     * @param chapterId the chapter id
     * @return the list of assessments
     */
    public List<GenericAssessmentResponse> getAssessmentsOfChapter(UUID userId, UUID chapterId) {
        try {
            WebClient webClient = WebClient.builder().baseUrl(contentServiceUrl).build();

            GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

            String query = """
                    query($userId: UUID!, $chapterIds: [UUID!]!) {
                        contentsByChapterIds(chapterIds: $chapterIds) {
                            ...on Assessment {
                                id
                                metadata {
                                    name
                                    tagNames
                                    suggestedDate
                                    type
                                    chapterId
                                    rewardPoints
                                }
                                assessmentMetadata {
                                    skillType
                                    skillPoints
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
                            }
                        }
                    }
                                    
                    """;

            log.info("Sending contentsByChapterIds query to course service with chapterId " + chapterId);

            // we must use media content here because the content type is an interface
            // that cannot be used for deserialization
            List<GenericAssessmentResponse> result = graphQlClient.document(query)
                    .variable("userId", userId)
                    .variable("chapterIds", List.of(chapterId))
                    .retrieve("contentsByChapterIds[0]")
                    .toEntityList(GenericAssessmentResponse.class)
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

    public static class GenericAssessmentResponse {
        private UUID id;
        private ContentMetadata metadata;
        private AssessmentMetadata assessmentMetadata;
        private UserProgressData progressDataForUser;

        public GenericAssessmentResponse() {
        }

        public GenericAssessmentResponse(UUID id,
                                         ContentMetadata metadata,
                                         AssessmentMetadata assessmentMetadata,
                                         UserProgressData progressDataForUser) {
            this.id = id;
            this.metadata = metadata;
            this.assessmentMetadata = assessmentMetadata;
            this.progressDataForUser = progressDataForUser;
        }

        public AssessmentMetadata getAssessmentMetadata() {
            return assessmentMetadata;
        }

        public UUID getId() {
            return id;
        }

        public ContentMetadata getMetadata() {
            return metadata;
        }

        public UserProgressData getProgressDataForUser() {
            return progressDataForUser;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenericAssessmentResponse that = (GenericAssessmentResponse) o;
            return Objects.equals(id, that.id) && Objects.equals(metadata, that.metadata) && Objects.equals(assessmentMetadata, that.assessmentMetadata) && Objects.equals(progressDataForUser, that.progressDataForUser);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, metadata, assessmentMetadata, progressDataForUser);
        }
    }

    public static class ContentServiceConnectionException extends RuntimeException {
        public ContentServiceConnectionException(String message, Exception e) {
            super(message, e);
        }
    }

}
