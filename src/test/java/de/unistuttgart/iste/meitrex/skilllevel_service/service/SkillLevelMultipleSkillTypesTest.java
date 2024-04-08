package de.unistuttgart.iste.meitrex.skilllevel_service.service;

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
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;


@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelMultipleSkillTypesTest {


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
    void testMultipleSkillTypes()  {
        UUID skillId=UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        UUID itemId2=UUID.randomUUID();
        final ArrayList<UUID> skillIds=new ArrayList<>();
         ArrayList<LevelOfBloomsTaxonomy> levels= new ArrayList<LevelOfBloomsTaxonomy>();
        final ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        levels.add(LevelOfBloomsTaxonomy.REMEMBER);
        skillIds.add(skillId);
        ItemResponse response1= ItemResponse.builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels).build();
        levels= new ArrayList<LevelOfBloomsTaxonomy>();
        levels.add(LevelOfBloomsTaxonomy.UNDERSTAND);
        ItemResponse response2= ItemResponse.builder()
                .itemId(itemId2)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels).build();
        responses.add(response1);
        responses.add(response2);
        skillLevelService.recalculateLevels(userId,responses);

        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).orElse(null);
        ItemDifficultyEntity itemDifficulty2=repoItemDifficulty.findById(itemId).orElse(null);
        ItemDifficultyEntity itemDifficulty3=repoItemDifficulty.findById(itemId).orElse(null);
        BloomLevelAbilityEntity bloomLevelAbility=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.REMEMBER);

        AllSkillLevelsEntity skillLevels=skillLevelsRepository.findById(new AllSkillLevelsEntity.PrimaryKey(skillId,userId)).get();
        float response =1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float normFactor= (float) (Math.abs(prediction-response)/(2*Math.abs(response-(0.5*prediction))));
        float newItemDifficulty= (float) ((1*(prediction-response)));
        float newSkillAbility= (float) (response-prediction)*normFactor;
        float skillLevelRemember= (float) (1/(1+Math.exp(-(0.5*newSkillAbility+0.5*newSkillAbility))));
        float responseSecond =1;
        float prediction2=(float) (1/(1+Math.exp(-newSkillAbility*0.5)));
        float newItemDifficulty2= (float) ((1)*(prediction2-responseSecond));
        normFactor= (float) (Math.abs(prediction2-responseSecond)/(Math.abs(responseSecond-(0.5*skillLevelRemember)+Math.abs(responseSecond-(0.5*0.5)))));
        float newSkillAbility2= (float) ( newSkillAbility+ (normFactor*((1/(1+0.05))*(responseSecond-prediction2))));
        float bloomAbility=(normFactor*(responseSecond-prediction2));
        float skillLevelUnderstand= (float) (1/(1+Math.exp(-(newSkillAbility2*0.5f+0.5f*bloomAbility))));
        assertThat(skillLevels.getRemember().getValue()).isEqualTo(skillLevelRemember);
        assertThat(skillLevels.getUnderstand().getValue()).isEqualTo(skillLevelUnderstand);
    }
}
