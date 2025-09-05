package de.unistuttgart.iste.meitrex.skilllevel_service.service.calculation;


import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.common.event.skilllevels.UserSkillLevelChangedEvent;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.BloomLevelAbilityRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillAbilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final TopicPublisher topicPublisher;

    private final static float ALPHA = 0.05f;
    private final static float BETA = 1f;

    //required percentage for correctness
    private final static float CORRCTNESS = (float) 3 / 4;

    /**
     * Recalculates the skill levels for a given user and responses.
     *
     * @param userId    The ID of the user
     * @param responses The responses of the current user
     */
    public void recalculateLevels(final UUID userId, final List<ItemResponse> responses) {
        for (ItemResponse response : responses) {
            calculate(userId, response);
        }
    }

    /**
     * updates for each response of the user the new difficulty of the item and the students abilities of the involved skills and levels of blooms Taxonomy
     *
     * @param userId   the id of the user
     * @param response the users response
     * @return Returns the passed AllSkillLevelsEntity with the added points.
     */
    private void calculate(final UUID userId, final ItemResponse response) {
        Optional<ItemDifficultyEntity> itemDifficultyEntity = itemDifficultyRepository.findById(response.getItemId());
        ItemDifficultyEntity itemDifficulty;
        if (itemDifficultyEntity.isEmpty()) {
            itemDifficulty = createItem(response.getItemId());
        } else {
            itemDifficulty = itemDifficultyEntity.get();
        }
        float difficulty = itemDifficulty.getDifficulty();
        List<Float> skillAbilities = new ArrayList<Float>();
        for (UUID skillId : response.getSkillIds()) {
            SkillAbilityEntity.PrimaryKey primaryKey = new SkillAbilityEntity.PrimaryKey(skillId, userId);
            Optional<SkillAbilityEntity> skillAbilityEntity = skillAbilityRepository.findById(primaryKey);
            SkillAbilityEntity ability;
            if (skillAbilityEntity.isEmpty()) {
                ability = createSkillAbility(userId, skillId);
            } else {
                ability = skillAbilityEntity.get();
            }
            skillAbilities.add(ability.getAbility());
        }
        List<Float> bloomLevelAbilities = new ArrayList<Float>();
        for (BloomLevel level : response.getLevelsOfBloomsTaxonomy()) {
            BloomLevelAbilityEntity ability = bloomLevelAbilityRepository.findByUserIdAndBloomLevel(userId, level);
            if (ability == null) {
                ability = createBloomAbility(userId, level);
            }
            bloomLevelAbilities.add(ability.getAbility());
        }
        float prediction = predict(difficulty, skillAbilities, bloomLevelAbilities);
        List<Float> abilities = Stream.concat(skillAbilities.stream(), bloomLevelAbilities.stream()).toList();
        float correctResponse = response.getResponse();
        if (correctResponse >= CORRCTNESS) {
            correctResponse = 1;
        } else {
            correctResponse = 0;
        }
        float alpha = calculateAlpha(prediction, correctResponse, difficulty, abilities);
        updateDifficulty(response.getItemId(), prediction, correctResponse);
        for (UUID skillId : response.getSkillIds()) {
            updateSkillAbility(userId, skillId, difficulty, correctResponse, alpha);
        }
        for (BloomLevel level : response.getLevelsOfBloomsTaxonomy()) {
            updateBloomLevelAbilities(userId, level, difficulty, correctResponse, alpha);
        }
        updateSkillLevels(response, userId, prediction);
    }

    /**
     * updates the SkillLevels for the given response
     *
     * @param response   the given response
     * @param userId     the id of the user
     * @param prediction the probability of a correct response predicted by M-Elo
     */
    private void updateSkillLevels(ItemResponse response, UUID userId, float prediction) {
        for (UUID skillId : response.getSkillIds()) {
            for (BloomLevel level : response.getLevelsOfBloomsTaxonomy()) {
                updateSkillLevel(userId, skillId, level, response.getItemId(), response.getResponse(), prediction);
            }
        }
    }

    /**
     * update the Skill Level of a given user and skill id
     *
     * @param userId       id of the user
     * @param skillId      id of the skill, the skilllevel belongs to
     * @param bloomLevel   level of Blooms Taxonomy
     * @param itemId       the id of the item
     * @param userResponse the response of the user
     * @param prediction   the probability of a correct response, predicted by M-Elo
     */

    private void updateSkillLevel(UUID userId, UUID skillId, BloomLevel bloomLevel, UUID itemId, float userResponse, float prediction) {
        Optional<AllSkillLevelsEntity> entityOptional = allSkillLevelsRepository.findById(new AllSkillLevelsEntity.PrimaryKey(skillId, userId));
        AllSkillLevelsEntity entity;
        if (entityOptional.isEmpty()) {
            entity = createAllSkillLevelsEntity(userId, skillId);
        } else {
            entity = entityOptional.get();
        }
        BloomLevelAbilityEntity bloomLevelAbility = bloomLevelAbilityRepository.findByUserIdAndBloomLevel(userId, bloomLevel);
        SkillAbilityEntity skillAbility = skillAbilityRepository.findById(new SkillAbilityEntity.PrimaryKey(skillId, userId)).get();
        float x = (float) 0.5f * skillAbility.getAbility() + 0.5f * bloomLevelAbility.getAbility();
        float newAbility = sigmoid(x);
        SkillLevelEntity skillLevelEntity = getSkillLevelEntityBySkillType(entity, bloomLevel);
        float oldAbility = skillLevelEntity.getValue();
        skillLevelEntity.setValue(newAbility);
        float difference = newAbility - oldAbility;
        SkillLevelLogEntry entry = new SkillLevelLogEntry();
        entry.setNewValue(newAbility);
        entry.setDifference(difference);
        entry.setAssociatedItemId(itemId);
        entry.setDate(OffsetDateTime.now());
        entry.setUserResponse(userResponse);
        entry.setPredictedCorrectness(prediction);
        List<SkillLevelLogEntry> log = skillLevelEntity.getLog();
        if (log != null) {
            log.add(entry);
        } else {
            log = new ArrayList<>();
            log.add(entry);
            skillLevelEntity.setLog(log);
        }
        setSkillLevelEntityBySkillType(entity, bloomLevel, skillLevelEntity);
        allSkillLevelsRepository.save(entity);
        topicPublisher.notifyUserSkillLevelChanged(UserSkillLevelChangedEvent.builder()
                .userId(userId)
                .skillId(skillId)
                .bloomLevel(bloomLevel)
                .oldValue(oldAbility)
                .newValue(newAbility)
                .build());
    }

    /**
     * Helper method which returns the correct SkillLevelEntity of an AllSkillLevelsEntity for a given skill type.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to get the SkillLevelEntity from
     * @param bloomLevel           The level of Blooms Taxonomy to get the SkillLevelEntity for
     * @return Returns the SkillLevelEntity of the passed AllSkillLevelsEntity for the passed skill type.
     */
    private SkillLevelEntity getSkillLevelEntityBySkillType(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                            final BloomLevel bloomLevel) {
        return switch (bloomLevel) {
            case UNDERSTAND -> allSkillLevelsEntity.getUnderstand();
            case REMEMBER -> allSkillLevelsEntity.getRemember();
            case APPLY -> allSkillLevelsEntity.getApply();
            case ANALYZE -> allSkillLevelsEntity.getAnalyze();
            case EVALUATE -> allSkillLevelsEntity.getEvaluate();
            case CREATE -> allSkillLevelsEntity.getCreate();
        };
    }

    /**
     * Helper method which sets the correct SkillLevelEntity of an AllSkillLevelsEntity for a given skill type.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to get the SkillLevelEntity from
     * @param bloomLevel           The level of Blooms Taxonomy to get the SkillLevelEntity for
     * @param entity               the new entity
     */

    private void setSkillLevelEntityBySkillType(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                final BloomLevel bloomLevel, final SkillLevelEntity entity) {
        switch (bloomLevel) {
            case UNDERSTAND -> allSkillLevelsEntity.setUnderstand(entity);
            case REMEMBER -> allSkillLevelsEntity.setRemember(entity);
            case APPLY -> allSkillLevelsEntity.setApply(entity);
            case ANALYZE -> allSkillLevelsEntity.setAnalyze(entity);
            case EVALUATE -> allSkillLevelsEntity.setEvaluate(entity);
            case CREATE -> allSkillLevelsEntity.setCreate(entity);
        }
        ;
    }

    /**
     * Helper method which calculates the sigmoid function for value x
     *
     * @param x the value for the sigmoid should be calculated
     * @return the calculated value
     */
    private float sigmoid(float x) {
        double value = 1 / (1 + Math.exp(-x));
        return (float) value;
    }

    /**
     * Helper method which calculates the probability, that a student will answer a given item correctly
     *
     * @param difficulty          difficulty of the given item
     * @param skillAbilities      the students' knowledge of the skills required to solve the item
     * @param bloomLevelAbilities the levels of Blooms Taxonomy, that are associated with the item
     * @return probability of a correct answer
     */
    private float predict(float difficulty, List<Float> skillAbilities, List<Float> bloomLevelAbilities) {
        float numberOfSkills = skillAbilities.size() + bloomLevelAbilities.size();
        float abilities = 0;
        //calculate the average of the skills
        for (float ability : skillAbilities) {
            abilities += ability * (1 / numberOfSkills);
        }
        for (float ability : bloomLevelAbilities) {
            abilities += ability * (1 / numberOfSkills);
        }
        return sigmoid(abilities - difficulty);
    }

    /**
     * Helper method which calculate the probability, that a student will answer a given item correctly,
     * when only one skill is required to solve the item
     *
     * @param difficulty the difficulty of the item
     * @param ability    the students' ability
     * @return the probability of a correct answer
     */
    private float predictOneParameter(float difficulty, float ability) {
        return sigmoid(ability - difficulty);
    }

    /**
     * Helper method which calculates the uncertainty function, values can be found via GridSearch
     *
     * @param attempts number of previous attempts
     */
    private float uncertaintyFunction(int attempts) {
        return BETA / (1 + ALPHA * attempts);
    }

    /**
     * Helper method that calculates s a normalization factor alpha, that is used for updating students' abilties
     * of skills when an item requires more than one ability
     *
     * @param prediction the probability of an correct answer
     * @param response   the students' response to the given item
     * @param difficulty the difficulty of the item
     * @param abilities  the students abilities
     * @return the normalization factor alpha
     */
    private float calculateAlpha(float prediction, float response, float difficulty, List<Float> abilities) {
        float sum = 0;
        float numberOfSkills = abilities.size();
        for (float ability : abilities) {
            sum += Math.abs(response - (predictOneParameter(difficulty, ability) * (1 / numberOfSkills)));
        }
        return Math.abs(prediction - response) / sum;
    }

    /**
     * Helper method which
     * updates the difficulty for the given item based on the newest response of the student
     * to the item and saves the new difficulty in the database
     *
     * @param itemId     id of the item
     * @param prediction probabilty of the correct answer, based on the students' abilities and the item difficulty
     * @param response   the actual answer of the student to the item
     */
    private void updateDifficulty(UUID itemId, float prediction, float response) {
        //get the item
        Optional<ItemDifficultyEntity> itemDifficultyEntityOptional = itemDifficultyRepository.findById(itemId);
        ItemDifficultyEntity item = itemDifficultyEntityOptional.get();
        int attempts = item.getNumberOfPreviousAttempts();
        float oldDifficulty = item.getDifficulty();
        float newDifficulty = oldDifficulty + uncertaintyFunction(attempts) * (prediction - response);
        item.setDifficulty(newDifficulty);
        item.setNumberOfPreviousAttempts(item.getNumberOfPreviousAttempts() + 1);
        itemDifficultyRepository.save(item);
    }

    /**
     * Helper method which
     * update the users' ability on the given skill based on the users latest response to an item requiring this skill
     *
     * @param userId     the id of the user
     * @param skillId    the id of the given skill
     * @param difficulty the difficulty of the item answered item
     * @param response   the users response
     */
    private void updateSkillAbility(UUID userId, UUID skillId, float difficulty, float response, float alpha) {
        SkillAbilityEntity skill = skillAbilityRepository.findById(new SkillAbilityEntity.PrimaryKey(skillId, userId)).get();
        float oldValue = skill.getAbility();
        float prediction = predictOneParameter(difficulty, oldValue);
        float newValue = oldValue + alpha * uncertaintyFunction(skill.getNumberOfPreviousAttempts()) * (response - prediction);
        skill.setAbility(newValue);
        System.out.println(newValue);
        skill.setNumberOfPreviousAttempts(skill.getNumberOfPreviousAttempts() + 1);
        skillAbilityRepository.save(skill);
    }

    /**
     * helper method which updates the users' ability for the given level of Blooms Taxonomy
     * based on the users latest response to an item requiring this bloom level
     *
     * @param userId     the id of the user
     * @param bloomLevel level of blooms taxonomy
     * @param difficulty the difficulty of the item answered item
     * @param response   the users response
     */
    private void updateBloomLevelAbilities(UUID userId, BloomLevel bloomLevel, float difficulty, float response, float alpha) {
        BloomLevelAbilityEntity bloomLevelAbility = bloomLevelAbilityRepository.findByUserIdAndBloomLevel(userId, bloomLevel);
        float oldValue = bloomLevelAbility.getAbility();
        float prediction = predictOneParameter(difficulty, oldValue);
        float newValue = oldValue + alpha * uncertaintyFunction(bloomLevelAbility.getNumberOfPreviousAttempts()) * (response - prediction);
        bloomLevelAbility.setNumberOfPreviousAttempts(bloomLevelAbility.getNumberOfPreviousAttempts() + 1);
        bloomLevelAbility.setAbility(newValue);
        bloomLevelAbilityRepository.save(bloomLevelAbility);
    }

    /**
     * helper methode that creates a new ItemDifficulty object with the given id and save the object in the database
     *
     * @param itemId given id of item
     * @return the new ItemDifficulty object
     */
    private ItemDifficultyEntity createItem(UUID itemId) {
        ItemDifficultyEntity itemDifficulty = new ItemDifficultyEntity(itemId, 0F, 0);
        itemDifficultyRepository.save(itemDifficulty);
        return itemDifficulty;
    }

    /**
     * crate a new SkillAbility Object with the given skill id and user id
     *
     * @param userId  the id of the user
     * @param skillId id of the skill
     * @return the new SkillAbility object
     */
    private SkillAbilityEntity createSkillAbility(UUID userId, UUID skillId) {
        SkillAbilityEntity ability = new SkillAbilityEntity(new SkillAbilityEntity.PrimaryKey(skillId, userId), 0F, 0);
        skillAbilityRepository.save(ability);
        return ability;
    }

    /**
     * create a new BloomLevelAbility Object for the given user and BloomLevel
     *
     * @param userId     the id of the user
     * @param bloomLevel the Level of Blooms Taxonomy
     * @return the new BloomLevelAbilityObject
     */
    private BloomLevelAbilityEntity createBloomAbility(UUID userId, BloomLevel bloomLevel) {
        BloomLevelAbilityEntity ability = new BloomLevelAbilityEntity(userId, bloomLevel, 0F, 0);
        ability = bloomLevelAbilityRepository.save(ability);
        return ability;
    }

    /**
     * create new AllSkillLevelsEntity and save it in the database
     *
     * @param userId  id of the user
     * @param skillId id of the skill
     * @return the newly created AllSkillLevelsEntity
     */
    private AllSkillLevelsEntity createAllSkillLevelsEntity(UUID userId, UUID skillId) {
        AllSkillLevelsEntity newEntity = new AllSkillLevelsEntity();
        newEntity.setId(new AllSkillLevelsEntity.PrimaryKey(skillId, userId));
        newEntity.setRemember(initializeSkillLevelEntity(0));
        newEntity.setUnderstand(initializeSkillLevelEntity(0));
        newEntity.setApply(initializeSkillLevelEntity(0));
        newEntity.setAnalyze(initializeSkillLevelEntity(0));
        newEntity.setEvaluate(initializeSkillLevelEntity(0));
        newEntity.setCreate(initializeSkillLevelEntity(0));
        // store in the db
        newEntity = allSkillLevelsRepository.save(newEntity);
        return newEntity;
    }

    /**
     * Initializes a skill level entity with the given initial value.
     *
     * @param initialValue The initial value to set the skill level to
     * @return The initialized skill level entity
     */
    private SkillLevelEntity initializeSkillLevelEntity(final float initialValue) {
        final SkillLevelEntity skillLevelEntity = new SkillLevelEntity(initialValue);
        skillLevelEntity.setValue(initialValue);
        skillLevelEntity.setLog(new ArrayList<>());
        return skillLevelEntity;
    }
}
