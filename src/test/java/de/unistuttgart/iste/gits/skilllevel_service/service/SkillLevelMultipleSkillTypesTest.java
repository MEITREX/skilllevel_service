package de.unistuttgart.iste.gits.skilllevel_service.service;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.skilllevel_service.test_util.MockContentServiceClientConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@ContextConfiguration(classes = {MockContentServiceClientConfiguration.class})
@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelMultipleSkillTypesTest {

    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private SkillLevelService skillLevelService;

    @Test
    @Transactional
    @Commit
    void testMultipleSkillTypes() {
        UUID chapterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // let's create some content, so we can calculate the user's skill levels based on their progress with it
        UUID content1Id = UUID.randomUUID();
        UUID content2Id = UUID.randomUUID();
        UUID content3Id = UUID.randomUUID();

        List<Content> contents = List.of(
                FlashcardSetAssessment.builder()
                        .setId(content1Id)
                        .setMetadata(ContentMetadata.builder()
                                .setChapterId(chapterId)
                                .setName("Content 1")
                                .setRewardPoints(5)
                                .setSuggestedDate(OffsetDateTime.of(2023,
                                        8,
                                        14,
                                        2,
                                        1,
                                        0,
                                        0,
                                        ZoneOffset.UTC))
                                .build())
                        .setAssessmentMetadata(AssessmentMetadata.builder()
                                .setSkillPoints(3)
                                .setSkillTypes(List.of(SkillType.REMEMBER))
                                .setInitialLearningInterval(1)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(content1Id)
                                .setLog(List.of(
                                        ProgressLogItem.builder()
                                                .setCorrectness(1)
                                                .setHintsUsed(0)
                                                .setTimestamp(OffsetDateTime.of(2023,
                                                        8,
                                                        14,
                                                        2,
                                                        1,
                                                        0,
                                                        0,
                                                        ZoneOffset.UTC))
                                                .setSuccess(true)
                                                .build(),
                                        ProgressLogItem.builder()
                                                .setCorrectness(1)
                                                .setHintsUsed(0)
                                                .setTimestamp(OffsetDateTime.of(2023,
                                                        8,
                                                        15,
                                                        5,
                                                        1,
                                                        0,
                                                        0,
                                                        ZoneOffset.UTC))
                                                .setSuccess(true)
                                                .build()))
                                .build())
                        .build(),
                FlashcardSetAssessment.builder()
                        .setId(content2Id)
                        .setMetadata(ContentMetadata.builder()
                                .setChapterId(chapterId)
                                .setName("Content 2")
                                .setRewardPoints(5)
                                .setSuggestedDate(OffsetDateTime.of(2023,
                                        8,
                                        14,
                                        2,
                                        1,
                                        0,
                                        0,
                                        ZoneOffset.UTC))
                                .build())
                        .setAssessmentMetadata(AssessmentMetadata.builder()
                                .setSkillPoints(7)
                                .setSkillTypes(List.of(SkillType.UNDERSTAND))
                                .setInitialLearningInterval(1)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(content2Id)
                                .setLog(List.of(
                                        ProgressLogItem.builder()
                                                .setCorrectness(1)
                                                .setHintsUsed(0)
                                                .setTimestamp(OffsetDateTime.of(2023,
                                                        8,
                                                        14,
                                                        2,
                                                        1,
                                                        0,
                                                        0,
                                                        ZoneOffset.UTC))
                                                .setSuccess(true)
                                                .build()))
                                .build())
                        .build(),
                FlashcardSetAssessment.builder()
                        .setId(content3Id)
                        .setMetadata(ContentMetadata.builder()
                                .setChapterId(chapterId)
                                .setName("Content 2")
                                .setRewardPoints(5)
                                .setSuggestedDate(OffsetDateTime.of(2023,
                                        8,
                                        14,
                                        2,
                                        1,
                                        0,
                                        0,
                                        ZoneOffset.UTC))
                                .build())
                        .setAssessmentMetadata(AssessmentMetadata.builder()
                                .setSkillPoints(3)
                                .setSkillTypes(List.of(SkillType.UNDERSTAND))
                                .setInitialLearningInterval(1)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(content3Id)
                                .setLog(List.of())
                                .build())
                        .build()
        );

        when(contentServiceClient.getContentsOfChapter(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        // skill level for remember, based on content 1.
        // The user has answered correctly twice, so the skill level should be 10 * (2/3)
        float skillLevelRemember = 10 * (2.f / 3);
        // skill level for understand, based on content 2 and 3.
        // Content 3 has not been answered yet, only content 2. So of the 10 levels of the chapter content 2 will
        // contribute 7/10 and content 2 was repeated once, so the skill level should be 7 * (1/3)
        float skillLevelUnderstand = 7 * (1.f / 3);

        assertThat(skillLevels.getRemember().getValue()).isEqualTo(skillLevelRemember);
        assertThat(skillLevels.getUnderstand().getValue()).isEqualTo(skillLevelUnderstand);
    }
}
