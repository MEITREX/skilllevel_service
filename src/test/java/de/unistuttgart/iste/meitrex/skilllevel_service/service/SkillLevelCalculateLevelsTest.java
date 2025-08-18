package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.BloomLevelAbilityRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillAbilityRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import java.util.ArrayList;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelCalculateLevelsTest {


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

    @Test
    @Transactional
    @Commit
    void testCalculateLevels() {
        // let's create some content, so we can calculate the user's skill levels based on their progress with it
        final UUID courseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID skillId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        final UUID itemId2 = UUID.randomUUID();
        final ArrayList<UUID> skillIds = new ArrayList<>();
        final ArrayList<BloomLevel> levels = new ArrayList<BloomLevel>();
        final ArrayList<ItemResponse> responses = new ArrayList<ItemResponse>();
        levels.add(BloomLevel.REMEMBER);
        levels.add(BloomLevel.ANALYZE);
        skillIds.add(skillId);
        ItemResponse response1 = ItemResponse.builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels).build();
        ItemResponse response2 = ItemResponse.builder()
                .itemId(itemId2)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels).build();
        responses.add(response1);
        responses.add(response2);
        skillLevelService.recalculateLevels(userId, responses);

        SkillAbilityEntity skillAbility = repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId, userId)).get();
        ItemDifficultyEntity itemDifficulty = repoItemDifficulty.findById(itemId).orElse(null);
        ItemDifficultyEntity itemDifficulty2 = repoItemDifficulty.findById(itemId).orElse(null);
        BloomLevelAbilityEntity bloomLevelAbility = repoBloomAbility.findByUserIdAndBloomLevel(userId, BloomLevel.REMEMBER);
        BloomLevelAbilityEntity bloomLevelAbility2 = repoBloomAbility.findByUserIdAndBloomLevel(userId, BloomLevel.ANALYZE);
        assertNotNull(itemDifficulty);
        assertNotNull(itemDifficulty2);
        assertNotNull(bloomLevelAbility);
        assertNotNull(bloomLevelAbility2);
        //make sure, that the new skill and item have the correct value
        float response = 1;
        float responseSecond = 0;
        float prediction = (float) (1 / (1 + (Math.exp(-0))));
        float newItemDifficulty = (prediction - response);
        float normFactor = (Math.abs(prediction - response) / (3 * Math.abs(response - ((1 / 3F) * prediction))));
        float newSkillAbility = (response - prediction) * normFactor;
        float newBloomAbility = (response - prediction) * normFactor;
        float newBloomAbility2 = (response - prediction) * normFactor;
        //calculate second itemA
        prediction = (float) (1 / (1 + (Math.exp(-((1 / 3F) * newSkillAbility + (1 / 3F) * newBloomAbility + (1 / 3F) * newBloomAbility2)))));
        float newItemDifficulty2 = (prediction - responseSecond);
        float oneParameterPrediction = (float) (1 / (1 + Math.exp(-(newSkillAbility))));
        normFactor = (float) (Math.abs(prediction - responseSecond) / (3 * Math.abs(responseSecond - ((1 / 3F) * oneParameterPrediction))));
        float newSkillAbilitySecondItem = (float) (newSkillAbility + ((1 / (1 + 0.05)) * normFactor * (responseSecond - prediction)));
        float newBloomAbilitySecondItem = (float) (newBloomAbility + ((1 / (1 + 0.05)) * normFactor * (responseSecond - prediction)));
        float newBloomAbility2SecondItem = (float) (newBloomAbility2 + ((1 / (1 + 0.05)) * normFactor * (responseSecond - prediction)));
        AllSkillLevelsEntity skillLevels = skillLevelsRepository.findById(new AllSkillLevelsEntity.PrimaryKey(skillId, userId)).get();
        float skillValueRemember = (float) (1 / (1 + Math.exp(-(0.5 * newBloomAbility + 0.5 * newSkillAbility))));
        float skillValueAnalyze = (float) (1 / (1 + Math.exp(-(0.5 * newBloomAbility + 0.5 * newSkillAbility))));
        float skillValueRemember2 = (float) (1 / (1 + Math.exp(-(0.5 * newBloomAbilitySecondItem + 0.5 * newSkillAbilitySecondItem))));
        float skillValueAnalyze2 = (float) (1 / (1 + Math.exp(-(0.5 * newBloomAbility2SecondItem + 0.5 * newSkillAbilitySecondItem))));
        assertThat(skillLevels.getRemember().getValue()).isEqualTo(skillValueRemember2);
        assertThat(skillLevels.getAnalyze().getValue()).isEqualTo(skillValueAnalyze2);

        // check the log
        assertThat(skillLevels.getRemember().getLog().get(0)).isEqualTo(
                new SkillLevelLogEntry(skillLevels.getRemember().getLog().get(0).getId(),
                        skillLevels.getRemember().getLog().get(0).getDate(),
                        skillValueRemember,
                        (double) skillValueRemember,
                        itemId,
                        1F,
                        0.5F)
        );
        assertThat(skillLevels.getAnalyze().getLog().get(0)).isEqualTo(
                new SkillLevelLogEntry(skillLevels.getAnalyze().getLog().get(0).getId(),
                        skillLevels.getAnalyze().getLog().get(0).getDate(),
                        skillValueAnalyze,
                        (double) skillValueAnalyze,
                        itemId,
                        1F,
                        0.5F));
        //double skillValueRememberSecondItem=  (1/Math.exp(-(newBloomAbilitySecondItem+newSkillAbilitySecondItem)));
        //double skillValueAnalyzeSecondItem= (1/Math.exp(-(newBloomAbility2SecondItem+newSkillAbilitySecondItem)));
        // check the log
        assertThat(skillLevels.getRemember().getLog().get(1)).isEqualTo(
                new SkillLevelLogEntry(skillLevels.getRemember().getLog().get(1).getId(),
                        skillLevels.getRemember().getLog().get(1).getDate(),
                        skillValueRemember2 - skillValueRemember,
                        skillValueRemember2,
                        itemId2,
                        0,
                        prediction));
        assertThat(skillLevels.getAnalyze().getLog().get(1)).isEqualTo(
                new SkillLevelLogEntry(skillLevels.getAnalyze().getLog().get(1).getId(),
                        skillLevels.getAnalyze().getLog().get(1).getDate(),
                        skillValueAnalyze2 - skillValueAnalyze,
                        skillValueAnalyze2,
                        itemId2,
                        0,
                        prediction)
        );
    }
}
