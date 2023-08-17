package de.unistuttgart.iste.gits.skilllevel_service;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.mapper.SkillLevelMapper;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.gits.skilllevel_service.service.ContentServiceClient;
import de.unistuttgart.iste.gits.skilllevel_service.service.SkillLevelService;
import de.unistuttgart.iste.gits.skilllevel_service.service.calculation.SkillLevelCalculator;
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
class SkillLevelServiceTest {

    @Autowired
    private AllSkillLevelsRepository repository;
    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private SkillLevelMapper skillLevelMapper;
    @Autowired
    private SkillLevelCalculator skillLevelCalculator;
    @Autowired
    private SkillLevelService skillLevelService;

    @Test
    @Transactional
    @Commit
    void testGetNonexistentSkillLevels() {
        UUID chapterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SkillLevels skillLevels = skillLevelService.getSkillLevels(chapterId, userId);

        assertThat(skillLevels.getAnalyze().getValue()).isZero();
        assertThat(skillLevels.getApply().getValue()).isZero();
        assertThat(skillLevels.getUnderstand().getValue()).isZero();
        assertThat(skillLevels.getRemember().getValue()).isZero();

        // assert that stuff has been placed in the db correctly as well
        AllSkillLevelsEntity entity = repository.findById(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId))
                .orElseThrow();

        assertThat(entity.getId().getChapterId()).isEqualTo(chapterId);
        assertThat(entity.getId().getUserId()).isEqualTo(userId);

        assertThat(entity.getUnderstand().getValue()).isZero();
        assertThat(entity.getAnalyze().getValue()).isZero();
        assertThat(entity.getRemember().getValue()).isZero();
        assertThat(entity.getApply().getValue()).isZero();
    }

    @Test
    @Transactional
    @Commit
    void testCalculateLevels() {
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
                                .setSkillType(SkillType.REMEMBER)
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
                                .setSkillType(SkillType.REMEMBER)
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

        when(contentServiceClient.getContentsWithUserProgressData(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        // 10 levels can be gained per chapter. The content we completed gives 3 of 3+4=7 skill points of
        // the chapter, and we completed it the first time so multiply by (1/3)
        float skillValue = 10 * (3.f / (3 + 4)) * (1.f / 3);

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

    @Test
    @Transactional
    @Commit
    void testRepetitionNotEnoughTimePassed() {
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
                                .setSkillType(SkillType.REMEMBER)
                                .setInitialLearningInterval(1)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(contentId1)
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
                                                        14,
                                                        5,
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
                                .setSkillType(SkillType.REMEMBER)
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

        when(contentServiceClient.getContentsWithUserProgressData(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        // 10 levels can be gained per chapter. The content we completed gives 3 of 3+4=7 skill points of
        // the chapter, and we completed it the first time so multiply by (1/3)
        // The second completion doesn't count because it's too soon after the first
        float skillValue = 10 * (3.f / (3 + 4)) * (1.f / 3);

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

    @Test
    @Transactional
    @Commit
    void testRepetitionEnoughTimePassed() {
        UUID chapterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // let's create some content, so we can calculate the user's skill levels based on their progress with it
        UUID contentId = UUID.randomUUID();

        List<Content> contents = List.of(
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
                                .setSkillType(SkillType.REMEMBER)
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

        when(contentServiceClient.getContentsWithUserProgressData(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        float skillValue1 = 10 * (1.f / 3);
        float skillValue2 = 10 * (2.f / 3);

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

    @Test
    @Transactional
    @Commit
    void testRepetitionNotEnoughTimePassedImproved() {
        UUID chapterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // let's create some content, so we can calculate the user's skill levels based on their progress with it
        UUID contentId = UUID.randomUUID();

        List<Content> contents = List.of(
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
                                .setSkillType(SkillType.REMEMBER)
                                .setInitialLearningInterval(1)
                                .build())
                        .setUserProgressData(UserProgressData.builder()
                                .setUserId(userId)
                                .setContentId(contentId)
                                .setLog(List.of(
                                        ProgressLogItem.builder()
                                                .setCorrectness(0.5)
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
                                                        14,
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

        when(contentServiceClient.getContentsWithUserProgressData(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        float skillValue1 = 10 * 0.5f * (1.f / 3);
        float skillValue2 = 10 * 1.f * (1.f / 3);

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
