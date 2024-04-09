package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.common.event.LevelOfBloomsTaxonomy;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.BloomLevelAbilityEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.ItemDifficultyEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAbilityEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.BloomLevelAbilityRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillAbilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;


@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
public class MElo_Test {

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

    /**
     * start simple. Start by testing if the addition of new skill works
     */
    @Test
    @Transactional
    @Commit
    public void testNewSkill(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        //make sure, that the item and the skill is not saved in the database
        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        //give the new assessment to M-Elo to update the item and skill
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertNotNull(itemDifficulty);
        assertNotNull(skillAbility);
        //make sure, that the new skill and item have the correct value
        float response =1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float newItemDifficulty=  ((prediction-response));
        System.out.println(response);
        System.out.println(newItemDifficulty);
        System.out.println(prediction);
        float newSkillAbility= ((response-prediction));
        System.out.println(itemDifficulty);
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());

        //continue with an incorrect response
        skillId=UUID.randomUUID();
        userId=UUID.randomUUID();
        itemId=UUID.randomUUID();
        skillIds=new ArrayList<>();
        ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        skillIds.add(skillId);
        levels= new ArrayList<LevelOfBloomsTaxonomy>();
        responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(0).skillIds(skillIds).levelsOfBloomsTaxonomy(levels).build();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertNotNull(itemDifficulty);
        assertNotNull(skillAbility);
        //make sure, that the new skill and item have the correct value

        response =0;
        prediction= (float) (1/(1+(Math.exp(-0))));
        newItemDifficulty= (float) ((prediction-response));
        newSkillAbility= (float) ((response-prediction));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());

    }
    @Test
    @Transactional
    @Commit
    public void testTwoNewSkill(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId=UUID.randomUUID();
        UUID skillId2=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        //make sure, that the item and the skill is not saved in the database
        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        Optional<SkillAbilityEntity> ability2=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId));
        assertFalse(ability2.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        //give the new assessment to M-Elo to update the item and skill
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId);
        skillIds.add(skillId2);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        SkillAbilityEntity skillAbility2= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertNotNull(itemDifficulty);
        assertNotNull(skillAbility);
        assertNotNull(skillAbility2);
        //make sure, that the new skill and item have the correct value
        float response =1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float newItemDifficulty=  ((prediction-response));
        float normFactor= (float) ( Math.abs(prediction-response)/Math.abs(2*(response-0.5*prediction)));
        float newSkillAbility= (normFactor*(response-prediction));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newSkillAbility).isEqualTo(skillAbility2.getAbility());

        //continue with an incorrect response
        skillId=UUID.randomUUID();
        skillId2=UUID.randomUUID();
        userId=UUID.randomUUID();
        itemId=UUID.randomUUID();
        skillIds=new ArrayList<>();
        ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        ability2=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId));
        assertFalse(ability2.isPresent());
        skillIds.add(skillId);
        skillIds.add(skillId2);
        levels= new ArrayList<LevelOfBloomsTaxonomy>();
        responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(0).skillIds(skillIds).levelsOfBloomsTaxonomy(levels).build();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        skillAbility2=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId)).get();
        itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertNotNull(itemDifficulty);
        assertNotNull(skillAbility);
        assertNotNull(skillAbility2);
        //make sure, that the new skill and item have the correct value
        response=0;
        prediction= (float) (1/(1+(Math.exp(-0))));
        newItemDifficulty=  ((prediction-response));
        normFactor= (float) ( Math.abs(prediction-response)/Math.abs(2*(response-0.5*prediction)));
        newSkillAbility= (normFactor*(response-prediction));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newSkillAbility).isEqualTo(skillAbility2.getAbility());

    }
    /**
     * test, that M-Elo works correctly when a item with multiple new skills is given
     */
    @Test
    @Transactional
    @Commit
    public void testMultipleNewSkill(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId1=UUID.randomUUID();
        UUID skillId2=UUID.randomUUID();
        UUID skillId3=UUID.randomUUID();
        UUID skillId4=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId1,userId));
        assertFalse(ability.isPresent());
        Optional<SkillAbilityEntity> ability2=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId));
        assertFalse(ability2.isPresent());
        Optional<SkillAbilityEntity> ability3=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId3,userId));
        assertFalse(ability3.isPresent());
        Optional<SkillAbilityEntity> ability4=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId4,userId));
        assertFalse(ability4.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        //give the new assessment to M-Elo to update the item and skill
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId1);
        skillIds.add(skillId2);
        skillIds.add(skillId3);
        skillIds.add(skillId4);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId1,userId)).get();
        SkillAbilityEntity skillAbility2= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId)).get();
        SkillAbilityEntity skillAbility3= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId3,userId)).get();
        SkillAbilityEntity skillAbility4= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId4,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertNotNull(itemDifficulty);
        assertNotNull(skillAbility);
        assertNotNull(skillAbility2);
        assertNotNull(skillAbility3);
        assertNotNull(skillAbility4);
        //make sure, that the new skill and item have the correct value
        float response =1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float newItemDifficulty=  ((prediction-response));
        float normFactor= (float) ( Math.abs(prediction-response)/Math.abs(4*(response-0.25*prediction)));
        float newSkillAbility= (normFactor*(response-prediction));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newSkillAbility).isEqualTo(skillAbility2.getAbility());
        assertThat(newSkillAbility).isEqualTo(skillAbility3.getAbility());
        assertThat(newSkillAbility).isEqualTo(skillAbility4.getAbility());

    }


    /**
     * test that M-Elo works correctly, when multiple new items with the same skill are given
     */
    @Test
    @Transactional
    @Commit
    public void testMultipleNewItemsOneSkillPerItem(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        UUID itemId2=UUID.randomUUID();
        //make sure, that the item and the skill is not saved in the database

        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        Optional<ItemDifficultyEntity> difficulty2= repoItemDifficulty.findById(itemId2);
        assertFalse(difficulty2.isPresent());
        //give the new assessment to M-Elo to update the item and skill
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        ItemResponse responseItem2 = new ItemResponse().builder()
                .itemId(itemId2)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        responses.add(responseItem2);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        ItemDifficultyEntity itemDifficulty2=repoItemDifficulty.findById(itemId2).get();
        assertNotNull(itemDifficulty);
        assertNotNull(itemDifficulty2);
        assertNotNull(skillAbility);
        //make sure, that the new skill and item have the correct value
        float response =1;
        float response2=1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float newItemDifficulty= (prediction-response);
        float newSkillAbility= (response-prediction);
        prediction= (float) (1/(1+(Math.exp(-newSkillAbility))));
        float newItemDifficulty2= (prediction-response2);
        newSkillAbility= (float) (newSkillAbility+(1/(1+0.05))*(response2-prediction));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newItemDifficulty2).isEqualTo(itemDifficulty2.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());

        //continue with two incorrect response
        skillId=UUID.randomUUID();
        userId=UUID.randomUUID();
        itemId=UUID.randomUUID();
        itemId2=UUID.randomUUID();
         ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        difficulty2= repoItemDifficulty.findById(itemId2);
        assertFalse(difficulty2.isPresent());
        //give the new assessment to M-Elo to update the item and skill
        skillIds=new ArrayList<>();
        skillIds.add(skillId);
        levels= new ArrayList<LevelOfBloomsTaxonomy>();
        responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        responseItem2 = new ItemResponse().builder()
                .itemId(itemId2)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        responses.add(responseItem2);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        itemDifficulty=repoItemDifficulty.findById(itemId).get();
        itemDifficulty2=repoItemDifficulty.findById(itemId2).get();
        assertNotNull(itemDifficulty);
        assertNotNull(itemDifficulty2);
        assertNotNull(skillAbility);
        //make sure, that the new skill and item have the correct value
        response =0;
        response2=0;
        prediction= (float) (1/(1+(Math.exp(-0))));
        newItemDifficulty= (prediction-response);
        newSkillAbility= (response-prediction);
        prediction= (float) (1/(1+(Math.exp(-newSkillAbility))));
        newItemDifficulty2= (prediction-response2);
        newSkillAbility= (float) (newSkillAbility+(1/(1+0.05))*(response2-prediction));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newItemDifficulty2).isEqualTo(itemDifficulty2.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
    }
    @Test
    @Transactional
    @Commit
    public void testMultipleAttemptsOnTheSameItem(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();

        //make sure, that the item and the skill is not saved in the database
        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        //give the new assessment to M-Elo to update the item and skill
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ItemResponse responseItem2 = new ItemResponse().builder()
                .itemId(itemId)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        responses.add(responseItem2);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertNotNull(itemDifficulty);
        assertNotNull(skillAbility);
        //make sure, that the new skill and item have the correct value
        float response =1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float newItemDifficulty=  ((prediction-response));
        float newSkillAbility= ((response-prediction));
        response=0;
        prediction= (float) (1/(1+Math.exp(-(newSkillAbility-newItemDifficulty))));
        newItemDifficulty=newItemDifficulty+((prediction-response)*(1/(1+0.05F)));
        newSkillAbility=newSkillAbility+((response-prediction)*(1/(1+0.05F)));
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());

    }
    /**
     *So far the level of Blooms Taxonomy has been ignored. Test a single item with one skill and one item of blooms taxonomy, as well as two levels of Blooms Taxonomy
     */

    @Test
    @Transactional
    @Commit
    public void testNewSkillBloomsTaxonomy(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        LevelOfBloomsTaxonomy level= LevelOfBloomsTaxonomy.UNDERSTAND;
        //make sure, that the item and the skill is not saved in the database
        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        BloomLevelAbilityEntity bloomLevelAbility=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.UNDERSTAND);
        assertNull(bloomLevelAbility);
        //give the new assessment to M-Elo to update the item and skill
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        levels.add(level);
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        BloomLevelAbilityEntity bloomAbility=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.UNDERSTAND);
        assertNotNull(skillAbility);
        assertNotNull(bloomAbility);
        assertNotNull(difficulty);

        //make sure, that the new skill and item have the correct value
        float response =1;
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float normFactor= (float) (Math.abs(prediction-response)/(2*Math.abs(response-0.5*(prediction))));
        float newItemDifficulty=(prediction-response);
        float newSkillAbility= (normFactor)*(response-prediction);
        float newBloomAbility= (normFactor)*(response-prediction);
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newBloomAbility).isEqualTo(bloomAbility.getAbility());

        //continue with an incorrect response
        skillId=UUID.randomUUID();
        userId=UUID.randomUUID();
        itemId=UUID.randomUUID();
        level= LevelOfBloomsTaxonomy.UNDERSTAND;
        LevelOfBloomsTaxonomy level2= LevelOfBloomsTaxonomy.REMEMBER;
        levels= new ArrayList<LevelOfBloomsTaxonomy>();
        levels.add(level);
        levels.add(level2);
        ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        assertFalse(ability.isPresent());
        difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        bloomLevelAbility=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.UNDERSTAND);
        assertNull(bloomLevelAbility);
        BloomLevelAbilityEntity bloomLevelAbility2=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.REMEMBER);
        assertNull(bloomLevelAbility2);
        //give the new assessment to M-Elo to update the item and skill
        skillIds=new ArrayList<>();
        skillIds.add(skillId);
        responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        //make sure, that the new skill and item are saved in the database
        skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        itemDifficulty=repoItemDifficulty.findById(itemId).get();
        bloomAbility=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.UNDERSTAND);
        BloomLevelAbilityEntity bloomAbility2=repoBloomAbility.findByUserIdAndBloomLevel(userId,BloomLevel.REMEMBER);
        assertNotNull(skillAbility);
        assertNotNull(bloomAbility);
        assertNotNull(bloomAbility2);
        assertNotNull(difficulty);

        //make sure, that the new skill and item have the correct value
        response =0;
        prediction= (float) (1/(1+(Math.exp(0))));
        normFactor=Math.abs(prediction-response)/Math.abs((response-(1/3F)*prediction)*3);
        newItemDifficulty= (prediction-response);
        newSkillAbility= normFactor*(response-prediction);
        newBloomAbility= (normFactor)*(response-prediction);
        float newBloomAbility2= (normFactor)*(response-prediction);
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newBloomAbility).isEqualTo(bloomAbility.getAbility());
        assertThat(newBloomAbility2).isEqualTo(bloomAbility2.getAbility());


    }
    /**
     * test M-Elo with multiple new items. Each item has a different combination
     */
    @Test
    @Transactional
    @Commit
    public void testMultipleCombinations(){
        //start with a correct response
        //generate a new Assessment if with unknown skill and item
        UUID skillId1=UUID.randomUUID();
        UUID skillId2=UUID.randomUUID();
        UUID skillId3=UUID.randomUUID();
        UUID skillId4=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID(); //1,2,3 skill
        UUID itemId2=UUID.randomUUID();  //second skill
        UUID itemId3=UUID.randomUUID(); //all skills
        //make sure, that the item and the skill is not saved in the database
        Optional<SkillAbilityEntity> ability=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId1,userId));
        assertFalse(ability.isPresent());
        Optional<SkillAbilityEntity> ability2=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId));
        assertFalse(ability2.isPresent());
        Optional<SkillAbilityEntity> ability3=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId3,userId));
        assertFalse(ability3.isPresent());
        Optional<SkillAbilityEntity> ability4=repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId4,userId));
        assertFalse(ability4.isPresent());
        Optional<ItemDifficultyEntity> difficulty= repoItemDifficulty.findById(itemId);
        assertFalse(difficulty.isPresent());
        Optional<ItemDifficultyEntity> difficulty2= repoItemDifficulty.findById(itemId2);
        assertFalse(difficulty2.isPresent());
        Optional<ItemDifficultyEntity> difficulty3= repoItemDifficulty.findById(itemId3);
        assertFalse(difficulty3.isPresent());
        //make sure, that the new skill and item have the correct value
        float response =1;
        float response2=0;
        float response3=1;
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId1);
        skillIds.add(skillId2);
        skillIds.add(skillId3);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
         skillIds=new ArrayList<>();
        skillIds.add(skillId2);
        ItemResponse responseItem2 = new ItemResponse().builder()
                .itemId(itemId2)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        skillIds=new ArrayList<>();
        skillIds.add(skillId1);
        skillIds.add(skillId2);
        skillIds.add(skillId3);
        skillIds.add(skillId4);
        ItemResponse responseItem3 = new ItemResponse().builder()
                .itemId(itemId3)
                .response(1)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        responses.add(responseItem2);
        responses.add(responseItem3);
        skillLevelService.recalculateLevels(userId,responses);
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId1,userId)).get();
        SkillAbilityEntity skillAbility2= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId2,userId)).get();
        SkillAbilityEntity skillAbility3= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId3,userId)).get();
        SkillAbilityEntity skillAbility4= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId4,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        ItemDifficultyEntity itemDifficulty2=repoItemDifficulty.findById(itemId2).get();
        ItemDifficultyEntity itemDifficulty3=repoItemDifficulty.findById(itemId3).get();
        assertNotNull(itemDifficulty);
        assertNotNull(itemDifficulty2);
        assertNotNull(itemDifficulty3);
        assertNotNull(skillAbility);
        assertNotNull(skillAbility2);
        assertNotNull(skillAbility3);
        assertNotNull(skillAbility4);
        float prediction= (float) (1/(1+(Math.exp(-0))));
        float newItemDifficulty= (prediction-response);
        float normFactor= (Math.abs(prediction-response)/(3*Math.abs(response-((1/3F)*prediction))));
        float newSkillAbility= normFactor*(response-prediction);
        float newSkillAbility2= normFactor*(response-prediction);
        float newSkillAbility3= normFactor*(response-prediction);
        //calculate second item
        response=0;
        prediction= (float) (1/(1+(Math.exp(-newSkillAbility2))));
        float newItemDifficulty2=(prediction-response);
        newSkillAbility2= (newSkillAbility2+(1/(1+0.05F))*(response-prediction));
        //calculate third item
        float abilities=(newSkillAbility+newSkillAbility2+newSkillAbility3)/4;
        prediction= (float) (1/(1+(Math.exp(-abilities))));
        response=1;
        float newItemDifficulty3= (prediction-response);
        float skillAbility1Prediction= (float) (1/(1+Math.exp(-newSkillAbility3)));
        float skillAbility2Prediction=(float) (1/(1+Math.exp(-newSkillAbility2)));
        normFactor= (float) (Math.abs(prediction-response)/(Math.abs(response-(1/4F)*skillAbility2Prediction)+2*Math.abs(response-(1/4F)*skillAbility1Prediction)+Math.abs(response-(1/4F)*0.5)));
        newSkillAbility= newSkillAbility+((1/(1+0.05F)*normFactor))*(response-prediction);
        newSkillAbility2= newSkillAbility2+((1/(1+0.1F)*normFactor))*(response-prediction);
        newSkillAbility3=newSkillAbility3+ ((1/(1+0.05F)*normFactor))*(response-prediction);
        float newSkillAbility4=  (normFactor)*(response-prediction);
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());
        assertThat(newItemDifficulty2).isEqualTo(itemDifficulty2.getDifficulty());
        assertThat(newItemDifficulty3).isEqualTo(itemDifficulty3.getDifficulty());
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newSkillAbility2).isEqualTo(skillAbility2.getAbility());
        assertThat(newSkillAbility3).isEqualTo(skillAbility3.getAbility());
        assertThat(newSkillAbility4).isEqualTo(skillAbility4.getAbility());
    }
    /**
     * test, that the update of existing difficulties and abilities work
     */
    @Test
    @Transactional
    @Commit
    public void updateExistingDifficultiesAndSkills() {
        //create a new skillAbility and save the ability in the database
        UUID skillId=UUID.randomUUID();
        UUID userId=UUID.randomUUID();
        UUID itemId=UUID.randomUUID();
        SkillAbilityEntity ability=new SkillAbilityEntity();
        ability.setAbility(0.9F);
        ability.setId(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        ability.setNumberOfPreviousAttempts(0);
        ability=repoSkillAbility.save(ability);
        //create a new itemDifficulty object and save the item difficulty in the database
        ItemDifficultyEntity difficulty=new ItemDifficultyEntity();
        difficulty.setDifficulty(-1.3F);
        difficulty.setItemId(itemId);
        difficulty.setNumberOfPreviousAttempts(0);
        difficulty=repoItemDifficulty.save(difficulty);
        //make sure,that item difficulty and skill ability are saved in the database
        SkillAbilityEntity skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        ItemDifficultyEntity itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertThat(itemDifficulty).isEqualTo(difficulty);
        assertThat(skillAbility).isEqualTo(ability);
        ArrayList<UUID> skillIds=new ArrayList<>();
        skillIds.add(skillId);
        ArrayList<LevelOfBloomsTaxonomy>levels= new ArrayList<LevelOfBloomsTaxonomy>();
        ItemResponse responseItem = new ItemResponse().builder()
                .itemId(itemId)
                .response(0)
                .skillIds(skillIds)
                .levelsOfBloomsTaxonomy(levels)
                .build();
        ArrayList<ItemResponse>responses=new ArrayList<ItemResponse>();
        responses.add(responseItem);
        skillLevelService.recalculateLevels(userId,responses);
        float response =0;
        float prediction = (float) (1 / (1 + (Math.exp(-(0.9+1.3F)))));
        float newItemDifficulty = (-1.3F+  (prediction - response));
        float newSkillAbility =  (0.9F+ (response - prediction));
        skillAbility= repoSkillAbility.findById(new SkillAbilityEntity.PrimaryKey(skillId,userId)).get();
        itemDifficulty=repoItemDifficulty.findById(itemId).get();
        assertThat(newSkillAbility).isEqualTo(skillAbility.getAbility());
        assertThat(newItemDifficulty).isEqualTo(itemDifficulty.getDifficulty());


    }

}

