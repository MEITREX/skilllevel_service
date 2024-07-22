package de.unistuttgart.iste.meitrex.skilllevel_service.service;

<<<<<<< HEAD
import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.common.event.LevelOfBloomsTaxonomy;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.BloomLevelAbilityEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.ItemDifficultyEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAbilityEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.BloomLevelAbilityRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillAbilityRepository;
=======
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.meitrex.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.test_util.MockContentServiceClientConfiguration;
>>>>>>> main
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
<<<<<<< HEAD

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
=======
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
>>>>>>> main
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
<<<<<<< HEAD


=======
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@ContextConfiguration(classes = {MockContentServiceClientConfiguration.class})
>>>>>>> main
@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelMultipleSkillTypesTest {

<<<<<<< HEAD

    @Autowired
    private SkillLevelService skillLevelService;
    @Autowired
    private SkillAbilityRepository repoSkillAbility;
    @Autowired
    private ItemDifficultyRepository repoItemDifficulty;
    @Autowired
    private BloomLevelAbilityRepository repoBloomAbility;

    @Autowired
    private AllSkillLevelsRepository skillLevelsRepository;
=======
    @Autowired
    private ContentServiceClient contentServiceClient;
    @Autowired
    private SkillLevelService skillLevelService;
>>>>>>> main

    @Test
    @Transactional
    @Commit
<<<<<<< HEAD
    void testMultipleSkillTypes() {
        UUID skillId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID itemId2 = UUID.randomUUID();
        final ArrayList<UUID> skillIds = new ArrayList<>();
        ArrayList<LevelOfBloomsTaxonomy> levels = new ArrayList<LevelOfBloomsTaxonomy>();
        final ArrayList<ItemResponse> responses = new ArrayList<ItemResponse>();
        levels.add(LevelOfBloomsTaxonomy.REMEMBER);
        skillIds.add(skillId);
        ItemResponse response1 = ItemResponse.builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels).build();
        levels = new ArrayList<LevelOfBloomsTaxonomy>();
        levels.add(LevelOfBloomsTaxonomy.UNDERSTAND);
        ItemResponse response2 = ItemResponse.builder()
                .itemId(itemId2)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels).build();
        responses.add(response1);
        responses.add(response2);
        skillLevelService.recalculateLevels(userId, responses);

        SkillAbilityEntity skillAbility = repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId, userId)).get();
        ItemDifficultyEntity itemDifficulty = repoItemDifficulty.findById(itemId).orElse(null);
        ItemDifficultyEntity itemDifficulty2 = repoItemDifficulty.findById(itemId).orElse(null);
        ItemDifficultyEntity itemDifficulty3 = repoItemDifficulty.findById(itemId).orElse(null);
        BloomLevelAbilityEntity bloomLevelAbility = repoBloomAbility.findByUserIdAndBloomLevel(userId, BloomLevel.REMEMBER);

        AllSkillLevelsEntity skillLevels = skillLevelsRepository.findById(new AllSkillLevelsEntity.PrimaryKey(skillId, userId)).get();
        float response = 1;
        float prediction = (float) (1 / (1 + (Math.exp(-0))));
        float normFactor = (float) (Math.abs(prediction - response) / (2 * Math.abs(response - (0.5 * prediction))));
        float newItemDifficulty = (float) ((1 * (prediction - response)));
        float newSkillAbility = (float) (response - prediction) * normFactor;
        float skillLevelRemember = (float) (1 / (1 + Math.exp(-(0.5 * newSkillAbility + 0.5 * newSkillAbility))));
        float responseSecond = 1;
        float prediction2 = (float) (1 / (1 + Math.exp(-newSkillAbility * 0.5)));
        float newItemDifficulty2 = (float) ((1) * (prediction2 - responseSecond));
        normFactor = (float) (Math.abs(prediction2 - responseSecond) / (Math.abs(responseSecond - (0.5 * skillLevelRemember) + Math.abs(responseSecond - (0.5 * 0.5)))));
        float predictionSkill = (float) (1 / (1 + Math.exp(-(newSkillAbility))));
        System.out.println("test" + normFactor);
        float newSkillAbility2 = (float) (newSkillAbility + (normFactor * ((1 / (1 + 0.05)) * (responseSecond - predictionSkill))));
        float bloomAbility = (normFactor * (responseSecond - 0.5f));
        float skillLevelUnderstand = (float) (1 / (1 + Math.exp(-(newSkillAbility2 * 0.5f + 0.5f * bloomAbility))));
=======
    void testMultipleSkillTypes() throws ContentServiceConnectionException {
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

        when(contentServiceClient.queryContentsOfChapter(any(), any())).thenReturn(contents);

        SkillLevels skillLevels = skillLevelService.recalculateLevels(chapterId, userId);

        // skill level for remember, based on content 1.
        // The user has answered correctly twice, so the skill level should be 10 * (2/3)
        float skillLevelRemember = 10 * (2.f / 3);
        // skill level for understand, based on content 2 and 3.
        // Content 3 has not been answered yet, only content 2. So of the 10 levels of the chapter content 2 will
        // contribute 7/10 and content 2 was repeated once, so the skill level should be 7 * (1/3)
        float skillLevelUnderstand = 7 * (1.f / 3);

>>>>>>> main
        assertThat(skillLevels.getRemember().getValue()).isEqualTo(skillLevelRemember);
        assertThat(skillLevels.getUnderstand().getValue()).isEqualTo(skillLevelUnderstand);
    }
}
