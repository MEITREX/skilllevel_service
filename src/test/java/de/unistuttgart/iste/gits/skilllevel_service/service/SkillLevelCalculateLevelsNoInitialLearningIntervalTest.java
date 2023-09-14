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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {MockContentServiceClientConfiguration.class})
@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelCalculateLevelsNoInitialLearningIntervalTest {

    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private SkillLevelService skillLevelService;

    @Test
    @Transactional
    @Commit
    void testCalculateLevelsNoInitialLearningInterval() {
        UUID chapterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // let's create some content, so we can calculate the user's skill levels based on their progress with it
        UUID contentId1 = UUID.randomUUID();
        UUID contentId2 = UUID.randomUUID();
        List<Content> contents = List.of(
                FlashcardSetAssessment.builder()
                        .setId(contentId1)
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
                                .setInitialLearningInterval(null)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(contentId1)
                                .setLog(List.of(ProgressLogItem.builder()
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
                QuizAssessment.builder()
                        .setId(contentId2)
                        .setMetadata(ContentMetadata.builder()
                                .setChapterId(chapterId)
                                .setName("Content 2")
                                .setRewardPoints(3)
                                .setSuggestedDate(OffsetDateTime.of(2023,
                                        8,
                                        14,
                                        3,
                                        1,
                                        0,
                                        0,
                                        ZoneOffset.UTC))
                                .build())
                        .setAssessmentMetadata(AssessmentMetadata.builder()
                                .setSkillPoints(4)
                                .setSkillTypes(List.of(SkillType.REMEMBER))
                                .setInitialLearningInterval(null)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(contentId2)
                                .setLog(List.of(ProgressLogItem.builder()
                                        .setCorrectness(0)
                                        .setHintsUsed(0)
                                        .setTimestamp(OffsetDateTime.of(2023,
                                                8,
                                                14,
                                                3,
                                                1,
                                                0,
                                                0,
                                                ZoneOffset.UTC))
                                        .setSuccess(false)
                                        .build()))
                                .build())
                        .build()
        );

        when(contentServiceClient.getContentsOfChapter(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        // 10 levels can be gained per chapter. The content we completed gives 3 of 3+4=7 skill points of
        // the chapter, and we completed it the first time so multiply by (1/3)
        float skillValue = 10 * (3.f / (3 + 4));

        assertThat(skillLevels.getRemember().getValue()).isEqualTo(skillValue);

        // check the log
        assertThat(skillLevels.getRemember().getLog()).containsExactly(
                new SkillLevelLogItem(contents.get(0).getUserProgressData().getLog().get(0).getTimestamp(),
                        skillValue,
                        0,
                        skillValue,
                        List.of(contentId1))
        );
    }
}
