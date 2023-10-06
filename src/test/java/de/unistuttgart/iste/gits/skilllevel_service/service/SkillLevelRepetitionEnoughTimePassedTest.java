package de.unistuttgart.iste.gits.skilllevel_service.service;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.gits.content_service.exception.ContentServiceConnectionException;
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

@ContextConfiguration(classes = MockContentServiceClientConfiguration.class)
@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelRepetitionEnoughTimePassedTest {

    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private SkillLevelService skillLevelService;

    @Test
    @Transactional
    @Commit
    void testRepetitionEnoughTimePassed() throws ContentServiceConnectionException {
        final UUID chapterId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        // let's create some content, so we can calculate the user's skill levels based on their progress with it
        final UUID contentId = UUID.randomUUID();

        final List<Content> contents = List.of(
                FlashcardSetAssessment.builder()
                        .setId(contentId)
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
                                .setContentId(contentId)
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
                        .build()
        );

        when(contentServiceClient.queryContentsOfChapter(any(), any())).thenReturn(contents);

        final SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        // skill level after first repetition
        final float skillValue1 = 10 * (1.f / 3);
        // skill level after second repetition
        final float skillValue2 = 10 * (2.f / 3);

        assertThat(skillLevels.getRemember().getValue()).isEqualTo(skillValue2);

        // check the log
        assertThat(skillLevels.getRemember().getLog()).containsExactly(
                new SkillLevelLogItem(contents.get(0).getUserProgressData().getLog().get(0).getTimestamp(),
                        skillValue1,
                        0,
                        skillValue1,
                        List.of(contentId)),
                new SkillLevelLogItem(contents.get(0).getUserProgressData().getLog().get(1).getTimestamp(),
                        skillValue2 - skillValue1,
                        skillValue1,
                        skillValue2,
                        List.of(contentId))
        );
    }
}
