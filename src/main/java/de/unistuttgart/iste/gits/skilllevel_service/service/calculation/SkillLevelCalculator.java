package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;



import de.unistuttgart.iste.gits.common.event.ItemResponse;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.*;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.BloomLevelAbilityRepository;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.SkillAbilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
imp
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Calculates the skill levels of a user for a given chapter of a course.
 */
@Component
@RequiredArgsConstructor
public class SkillLevelCalculator {

    private final ItemDifficultyRepository itemDifficultyRepository;

    private final SkillAbilityRepository skillAbilityRepository;

    private final BloomLevelAbilityRepository bloomLevelAbilityRepository;

    private final AllSkillLevelsRepository allSkillLevelsRepository;

    private final static float ALPHA=0.05;
    private final static float BETA=1.0;

    //required percentage for correctness
    private final static float CORRCTNESS=0.75;

    /**
     * Recalculates the skill levels for a given user and responses.
     *
     * @param userId   The ID of the user
     * @param responses The responses of the current user
     * @param courseId the id of the course
     */
    public void recalculateLevels(final UUID userId,final List<ItemResponse> responses,UUID courseId) {
        for(ItemResponse response:responses){
            calculate(userId,response,courseId);
        }
    }

    /**
     * updates for each response of the user the new difficulty of the item and the students abilities of the involved skills and levels of blooms Taxonomy
     *
     * @param userId the id of the user
     * @param response the users response
     * @param courseId the id of the course, necessary for creating a new AllSkillLevelsEntity
     * @return Returns the passed AllSkillLevelsEntity with the added points.
     */
    private void calculate(final UUID userId,final ItemResponse response,final UUID courseId) {
        ItemDifficultyEntity itemDifficulty=itemDifficultyRepository.findById(response.getItemId()).orElse(()->createItem(item.getId()));
        float difficulty=itemDifficulty.getDifficulty();
        List<float>skillAbilities=new ArrayList<float>();
        for(UUID skillId:response.getSkillIds()){
            var primaryKey = new SkillAbilityEntity.PrimaryKey(skillId,userId);
            SkillAbility ability=skillAbilityRepository.findById(primaryKey).orElse(()->createSkillAbility(userId,skillId));
            skillAbilities.add(ability.getAbility());
        }
        List<float>bloomlevelAbilities=new ArrayList<float>();
        for(BloomLevel level:bloomLevels){
            //TODO:map Bloom Level from CloudEvent to BloomLevel of DTO
            BloomLevelAbility ability=bloomLevelAbilityRepository.findByUserIdAndBloomLevel(userId,level).orElse(()->createBloomAbility(userId,level));
            bloomlevelAbilities.add(ability.getAbility());
        }
        float prediction=predict(difficulty,skillAbilities,bloomlevelAbilities);
        List<float> abilities = Stream.concat(skillAbilities.stream(), bloomlevelAbilities.stream()).toList();
        int correctResponse=response.getResponse();
        float alpha =calculateAlpha(prediction,correctResponse,difficulty,abilities);
        updateDifficulty(response.getItemId(),prediction,correctResponse);
        for(UUID skillId:skillIds) {
            updateSkillAbility(userId, skillId, prediction,correctResponse,alpha);
        }
        for(BloomLevel level:bloomLevels){
            updateBloomLevelAbilities(userId,level,prediction,correctResponse,alpha);
        }
        updateSkillLevels(response,userId,courseId);
    }

    /**
     * updates the SkillLevels for the given response
     * @param response the given response
     * @param userId the id of the user
     * @param courseId the of the course the skill belongs to, necessary for creating a new AllSkillLevelsEntity
     */
    private void updateSkillLevels(ItemResponse response,UUID userId,UUID courseId){
        //TODO:map Bloom Level from CloudEvent to BloomLevel of DTO
        for(UUID skillId:response.getSkillIds()){
            for(BloomLevel bloomLevel:response.getBloomLevel()){
                    updateSkillLevel(userId,skillId,bloomLevel,response.getItemId(),courseId);
            }

        }
    }

    /**
     * update the Skill Level of a given user and skill id
     * @param userId id of the user
     * @param skillId id of the skill, the skilllevel belongs to
     * @param bloomLevel level of Blooms Taxonomy
     * @param itemId the id of the item
     * @param courseId the of the course the skill belongs to, necessary for creating a new AllSkillLevelsEntity
     */

    private void updateSkillLevel(UUID userId,UUID skillId, BloomLevel bloomLevel,UUID itemId,UUID courseId){
        AllSkillLevelsEntity entity= allSkillLevelsRepository.findById(new AllSkillLevelsEntity.PrimaryKey(skillId,userId)).orElse(createAllSkillLevelsEntity(userId,itemId,courseId));
        BloomLevelAbilityEntity bloomLevelAbility=bloomLevelAbilityRepository.findByUserIdAndBloomLevel(userId, bloomLevel);
        SkillAbilityEntity skillAbility=skillAbilityRepository.findById(skillId);
        float newAbility=sigmoid(0.5*skillAbility.getAbility()+0.5*bloomLevelAbility.getAbility());
        SkillLevelEntity skillLevelEntity=getSkillLevelEntityBySkillType(entity,bloomLevel);
        skillLevelEntity.setValue(newAbility);
        float oldAbility=skillLevelEntity.getValue();
        float difference=newAbility-oldAbility;
        SkillLevelLogEntry entry= new SkillLevelLogEntry();
        entry.setNewValue(newAbility);
        entry.setDifference(difference);
        entry.setAssociatedItemId(itemId);
        List<SkillLevelLogEntry>log=skillLevelEntity.getLog();
        if(log!=null){
            log.add(entry);
        }
        else{
            log=new ArrayList<>();
            log.add(entry);
            skillLevelEntity.setLog(log);
        }
        allSkillLevelsRepository.save(entity);

    }

    /**
     * Helper method which returns the correct SkillLevelEntity of an AllSkillLevelsEntity for a given skill type.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to get the SkillLevelEntity from
     * @param bloomLevel            The level of Blooms Taxonomy to get the SkillLevelEntity for
     * @return Returns the SkillLevelEntity of the passed AllSkillLevelsEntity for the passed skill type.
     */
    private SkillLevelEntity getSkillLevelEntityBySkillType(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                            final BloomLevel bloomLevel) {
        return switch (bloomLevel) {
            case UNDERSTAND -> allSkillLevelsEntity.getUnderstand();
            case REMEMBER -> allSkillLevelsEntity.getRemember();
            case APPLY -> allSkillLevelsEntity.getApply();
            case ANALYSE -> allSkillLevelsEntity.getAnalyze();
            case EVALUATE ->allSkillLevelsEntity.getEvaluate();
            case CREATE ->allSkillLevelsEntity.getCreate();
        };
    }

    /**
     * Helper method which calculates the sigmoid function for value x
     * @param x the value for the sigmoid should be calculated
     * @return the calculated value
     */
    private float sigmoid(float x){
        return 1/(1+Math.exp(-x));
    }

    /**
     * Helper method which calculates the probability, that a student will answer a given item correctly
     * @param difficulty difficulty of the given item
     * @param skillAbilities the students' knowledge of the skills required to solve the item
     * @param bloomLevelAbilities the levels of Blooms Taxonomy, that are associated with the item
     * @return probability of a correct answer
     */
    private float predict(float difficulty,List<float> skillAbilities, List<float> bloomLevelAbilities ){
        float numberOfSkills=skillAbilities.size()+bloomLevelAbilities.size();
        float abilities=0;
        //calculate the average of the skills
        for(float ability:skillAbilities){
            abilities+=ability*(1/numberOfSkills);
        }
        for(float ability:bloomLevelAbilities){
            abilities+=ability*(1/numberOfSkills);
        }
        return sigmoid(abilities-difficulty) ;
    }

    /**
     *Helper method which calculate the probability, that a student will answer a given item correctly,
     * when only one skill is required to solve the item
     * @param difficulty the difficulty of the item
     * @param ability the students' ability
     * @return the probability of a correct answer
     */
    private float predictOneParameter(float difficulty,float ability){
        return sigmoid(ability-difficulty);
    }

    /**
     * Helper method which calculates the uncertainty function, values can be found via GridSearch
     * @param attempts number of previous attempts
     */
    private float uncertaintyFunction(int attempts){
        return BETA/(1+ALPHA*attempts);
    }
    /**
     *Helper method that calculates s a normalization factor alpha, that is used for updating students' abilties
     * of skills when an item requires more than one ability
     * @param prediction the probability of an correct answer
     * @param response the students' response to the given item
     * @param difficulty the difficulty of the item
     * @param abilities the students abilities
     * @return the normalization factor alpha
     */
    private float calculateAlpha(float prediction,float response,float difficulty,List<float>abilities){
        float sum=0;
        float numberOfSkills=abilities.size();
        for(float ability:abilities){
            sum+=Math.abs(response-(predictOneParameter(difficulty,ability)*(1/numberOfSkills)));
        }
        return Math.abs(prediction-response)/sum;
    }
    /**
     * Helper method which
     * updates the difficulty for the given item based on the newest response of the student
     * to the item and saves the new difficulty in the database
     * @param itemId id of the item
     * @param prediction probabilty of the correct answer, based on the students' abilities and the item difficulty
     * @param response the actual answer of the student to the item
     */
    private void updateDifficulty(UUID itemId,float prediction, int response){
        //get the item
        ItemDifficultyEntity item=itemDifficultyRepository.findAllById(itemId);
        int attempts= item.getNumberOfPreviousAttempts();
        float oldDifficulty=item.getDifficulty();
        float newDifficulty=oldDifficulty+uncertaintyFunction(attempts)*(prediction-response);
        item.setDifficulty(newDifficulty);
        item.setNumberOfPreviousAttempts(item.getNumberOfPreviousAttempts()+1);
        itemDifficultyRepository.save(item);
    }

    /**
     * Helper method which
     * update the users' ability on the given skill based on the users latest response to an item requiring this skill
     * @param userId the id of the user
     * @param skillId the id of the given skill
     * @param prediction the probability, that the student will answer the latest item correctly
     * @param response the users response
     */
    private void updateSkillAbility(UUID userId,UUID skillId,float prediction, float response,float alpha){
        SkillAbility skill = skillAbilityRepository.findAllById(new SkillAbilityEntity.PrimaryKey(skillId,userId));
        float oldValue= skill.getAbility();
        float newValue=oldValue+alpha*uncertaintyFunction(skill.getNumberOfPreviousAttempts())*(response-prediction);
        skill.setAbility(newValue);
        skill.setNumberOfPreviousAttempts(skill.getNumberOfPreviousAttempts()+1);
        skillAbilityRepository.save(skill);
    }

    /**
     *helper method which updates the users' ability for the given level of Blooms Taxonomy
     * based on the users latest response to an item requiring this bloom level
     * @param userId the id of the user
     * @param bloomLevel level of blooms taxonomy
     * @param prediction the probability, that the student will answer the latest item correctly
     * @param response the users response
     */
    private void updateBloomLevelAbilities(UUID userId,BloomLevel bloomLevel,float prediction, float response, float alpha){
        BloomLevelAbility bloomLevelAbility = bloomLevelAbilityRepository.findByUserIdAndBloomLevel(userId,bloomLevel);
        float oldValue=bloomLevelAbility.getAbility();
        float newValue=oldValue+alpha*uncertaintyFunction(bloomLevelAbility.getNumberOfPreviousAttempts())*(response-prediction);
        bloomLevelAbility.setNumberOfPreviousAttempts(bloomLevelAbility.getNumberOfPreviousAttempts()+1);
        bloomLevelAbility.setAbility(newValue);
        bloomLevelAbilityRepository.save(bloomLevelAbility);
    }
    /**
     * helper methode that creates a new ItemDifficulty object with the given id and save the object in the database
     * @param itemId given id of item
     * @return the new ItemDifficulty object
     */
    private ItemDifficultyEntity createItem(UUID itemId){
        ItemDifficultyEntity itemDifficulty=new ItemDifficultyEntity(itemId,0.0,0);
        itemDifficultyRepository.save(itemDifficulty);
        return itemDifficulty;
    }

    /**
     * crate a new SkillAbility Object with the given skill id and user id
     * @param userId the id of the user
     * @param skillId id of the skill
     * @return the new SkillAbility object
     */
    private SkillAbilityEntity createSkillAbility(UUID userId, UUID skillId){
        SkillAbilityEntity ability=new SkillAbilityEntity(new SkillAbilityEntity.PrimaryKey(skillId,userId),0.0,0);
        skillAbilityRepository.save(ability);
        return ability;
    }

    /**
     * create a new BloomLevelAbility Object for the given user and BloomLevel
     * @param userId the id of the user
     * @param bloomLevel the Level of Blooms Taxonomy
     * @return the new BloomLevelAbilityObject
     */
    private BloomLevelAbilityEntity createBloomAbility(UUID userId,BloomLevel bloomLevel){
        BloomLevelAbilityEntity ability=new BloomLevelAbilityEntity(userId,bloomLevel,0.0,0);
        ability=bloomLevelAbilityRepository.save(ability);
        return ability;
    }

    /**
     * create new AllSkillLevelsEntity and save it in the database
     * @param userId id of the user
     * @param skillId id of the skill
     * @param courseId id of the course the item belongs to
     * @return the newly created AllSkillLevelsEntity
     */
    private AllSkillLevelsEntity createAllSkillLevelsEntity(UUID userId,UUID skillId,UUID courseId){
        AllSkillLevelsEntity newEntity = new AllSkillLevelsEntity();
        newEntity.setId(new AllSkillLevelsEntity.PrimaryKey(skillId,userId));
        newEntity.setRemember(initializeSkillLevelEntity(0.0));
        newEntity.setUnderstand(initializeSkillLevelEntity(0.0));
        newEntity.setApply(initializeSkillLevelEntity(0.0));
        newEntity.setAnalyze(initializeSkillLevelEntity(0.0));
        newEntity.setEvaluate(initializeSkillLevelEntity(0.0));
        newEntity.setCreate(initializeSkillLevelEntity(0.0));
        newEntity.setCreate(initializeSkillLevelEntity(0.0));
        // store in the db
        newEntity=skillLevelsRepository.save(newEntity);
        return  newEntity;
    }
    /**
     * Initializes a skill level entity with the given initial value.
     *
     * @param initialValue The initial value to set the skill level to
     * @return The initialized skill level entity
     */
    private SkillLevelEntity initializeSkillLevelEntity(final float initialValue) {
        final SkillLevelEntity skillLevelEntity = new SkillLevelEntity();
        skillLevelEntity.setValue(initialValue);
        skillLevelEntity.setLog(new ArrayList<>());
        return skillLevelEntity;
    }

}
