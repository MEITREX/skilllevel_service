package de.unistuttgart.iste.meitrex.skilllevel_service.service.calculation;

<<<<<<< HEAD

import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.common.event.LevelOfBloomsTaxonomy;
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
=======
import de.unistuttgart.iste.meitrex.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.meitrex.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.meitrex.generated.dto.Assessment;
import de.unistuttgart.iste.meitrex.generated.dto.Content;
import de.unistuttgart.iste.meitrex.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.meitrex.generated.dto.SkillType;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillLevelLogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
>>>>>>> main

/**
 * Calculates the skill levels of a user for a given chapter of a course.
 */
@Component
@RequiredArgsConstructor
public class SkillLevelCalculator {

<<<<<<< HEAD
    private final ItemDifficultyRepository itemDifficultyRepository;

    private final SkillAbilityRepository skillAbilityRepository;

    private final BloomLevelAbilityRepository bloomLevelAbilityRepository;

    private final AllSkillLevelsRepository allSkillLevelsRepository;

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
        for (LevelOfBloomsTaxonomy levelOfBloomsTaxonomy : response.getLevelsOfBloomsTaxonomy()) {
            BloomLevel level = mapLevelOfBloomsTaxonomyToBloomLevel(levelOfBloomsTaxonomy);
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
        for (LevelOfBloomsTaxonomy levelOfBloomsTaxonomy : response.getLevelsOfBloomsTaxonomy()) {
            BloomLevel level = mapLevelOfBloomsTaxonomyToBloomLevel(levelOfBloomsTaxonomy);
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
            for (LevelOfBloomsTaxonomy levelOfBloomsTaxonomy : response.getLevelsOfBloomsTaxonomy()) {
                BloomLevel level = mapLevelOfBloomsTaxonomyToBloomLevel(levelOfBloomsTaxonomy);
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
=======
    private final ContentServiceClient contentServiceClient;

    // 3 repetitions needed for full points.
    // TODO: This is hard-coded for now, but could be made configurable per assessment in the future
    static final int REPETITIONS_NEEDED = 3;

    /**
     * Recalculates the skill levels for a given user and chapter of a course.
     *
     * @param chapterId            The ID of the chapter
     * @param userId               The ID of the user
     * @param allSkillLevelsEntity The current skill levels of the user
     * @return The recalculated skill levels. The entity needs to be stored in the DB by the caller!
     * @throws ContentServiceConnectionException If the connection to the content service fails
     */
    public AllSkillLevelsEntity recalculateLevels(final UUID chapterId, final UUID userId,
                                                  final AllSkillLevelsEntity allSkillLevelsEntity)
            throws ContentServiceConnectionException {

        final List<Content> contents =
                contentServiceClient.queryContentsOfChapter(userId, chapterId);

        // set all skill levels to 0 and clear their logs. We're recalculating everything from scratch
        allSkillLevelsEntity.setAnalyze(new SkillLevelEntity(0));
        allSkillLevelsEntity.setRemember(new SkillLevelEntity(0));
        allSkillLevelsEntity.setUnderstand(new SkillLevelEntity(0));
        allSkillLevelsEntity.setApply(new SkillLevelEntity(0));

        return calculate(
                allSkillLevelsEntity,
                contents.stream()
                        .filter(Assessment.class::isInstance)
                        .map(x -> (Assessment) x)
                        .toList()
        );
    }

    /**
     * Adds the points earned by a user by working on the passed assessments to the passed skill levels.
     *
     * @param allSkillLevelsEntity The skill levels entity of the user to add the points to
     * @param assessments          The assessments to calculate the points from
     * @return Returns the passed AllSkillLevelsEntity with the added points.
     */
    private AllSkillLevelsEntity calculate(final AllSkillLevelsEntity allSkillLevelsEntity,
                                           final List<Assessment> assessments) {
        // find out the total amount of skill points in the current chapter
        final AllSkillPoints totalSkillPoints = getTotalSkillPointsOfAssessments(assessments);

        for (final Assessment assessment : assessments) {
            addLogItemsToSkillLevelsForAssessment(allSkillLevelsEntity, assessment, totalSkillPoints);
        }

        // go through all 4 types of skill levels and sort their log entries by date
        for (final SkillType skillType : SkillType.values()) {
            final SkillLevelEntity skillLevelToModify = getSkillLevelEntityBySkillType(allSkillLevelsEntity, skillType);

            final List<SkillLevelLogEntry> sortedLog = new ArrayList<>(skillLevelToModify.getLog().stream()
                    .sorted(Comparator.comparing(SkillLevelLogEntry::getDate))
                    .toList());

            // calculate the value differences between the log entries
            float skillLevel = 0;
            for (final SkillLevelLogEntry currentLogEntry : sortedLog) {
                skillLevel += currentLogEntry.getDifference();
                currentLogEntry.setNewValue(skillLevel);
            }

            skillLevelToModify.setLog(sortedLog);

            if (sortedLog.isEmpty()) {
                skillLevelToModify.setValue(0);
            } else {
                skillLevelToModify.setValue(sortedLog.get(sortedLog.size() - 1).getNewValue());
            }

        }

        return allSkillLevelsEntity;
    }

    /**
     * Helper method which, given an AllSkillLevelsEntity and an assessment, adds the earned skill points of the
     * assessment to the log of the corresponding skill levels. Note that the log items are just appended to the
     * log, they are not sorted by date yet, and they "newValue" property is not set yet, as it is dependent on
     * other values in the log.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to add the log items to
     * @param assessment           The assessment to calculate the log items from
     * @param totalSkillPoints     The total amount of skill points in the current chapter
     */
    private void addLogItemsToSkillLevelsForAssessment(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                       final Assessment assessment,
                                                       final AllSkillPoints totalSkillPoints) {
        final List<SkillType> skillTypes = assessment.getAssessmentMetadata().getSkillTypes();

        final List<AssessmentRepetition> repetitionResults = calculateSkillPointsOfRepetitions(assessment);

        double highestSkillPointsTillNow = 0;
        // go over all repetitions the user has made on this content
        for (final AssessmentRepetition currentRepetition : repetitionResults) {
            // only add this repetition to the skill level log if the user has improved compared to previous ones
            if (currentRepetition.earnedSkillPoints <= highestSkillPointsTillNow) {
                continue;
            }

            final List<UUID> contentIds = new ArrayList<>(1);
            contentIds.add(assessment.getId());

            // add the log entry to the skill level. For now only add the skill points earned ("difference")
            // because the order of the log will not be correct for now because we go through each assessment
            // one by one. Later we will sort the log entries by the timestamp and calculate the missing values
            for (final SkillType skillType : skillTypes) {
                // each chapter has a maximum of 10 skill levels, so we need to scale the earned skill points relative
                // to the total skill points of the chapter times the 10 levels to calculate how many levels the user
                // will gain
                final double relativeSkillPoints
                        = 10f * (currentRepetition.earnedSkillPoints() / totalSkillPoints.getValueBySkillType(skillType));
                final double relativeSkillPointsPrevious
                        = 10f * (highestSkillPointsTillNow / totalSkillPoints.getValueBySkillType(skillType));

                final SkillLevelEntity skillLevelToModify = getSkillLevelEntityBySkillType(allSkillLevelsEntity, skillType);
                skillLevelToModify.getLog().add(SkillLevelLogEntry.builder()
                        .date(currentRepetition.timestamp)
                        .difference((float) (relativeSkillPoints - relativeSkillPointsPrevious))
                        .associatedContentIds(contentIds)
                        .build());
            }

            highestSkillPointsTillNow = currentRepetition.earnedSkillPoints;
        }
>>>>>>> main
    }

    /**
     * Helper method which returns the correct SkillLevelEntity of an AllSkillLevelsEntity for a given skill type.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to get the SkillLevelEntity from
<<<<<<< HEAD
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
=======
     * @param skillType            The skill type to get the SkillLevelEntity for
     * @return Returns the SkillLevelEntity of the passed AllSkillLevelsEntity for the passed skill type.
     */
    private SkillLevelEntity getSkillLevelEntityBySkillType(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                            final SkillType skillType) {
        return switch (skillType) {
            case UNDERSTAND -> allSkillLevelsEntity.getUnderstand();
            case REMEMBER -> allSkillLevelsEntity.getRemember();
            case APPLY -> allSkillLevelsEntity.getApply();
            case ANALYSE -> allSkillLevelsEntity.getAnalyze();
>>>>>>> main
        };
    }

    /**
<<<<<<< HEAD
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

    /***
     * maps the Level of Blooms Taxonomy from Dapr Topic to the same Level of Blooms Taxonomy of the DTO
     * @param levelOfBloomsOfBloomsTaxonomy level of blooms Taxonomy
     * @return mapped level for the DTO
     */
    private BloomLevel mapLevelOfBloomsTaxonomyToBloomLevel(LevelOfBloomsTaxonomy levelOfBloomsOfBloomsTaxonomy) {
        return switch (levelOfBloomsOfBloomsTaxonomy) {
            case LevelOfBloomsTaxonomy.UNDERSTAND -> BloomLevel.UNDERSTAND;
            case LevelOfBloomsTaxonomy.REMEMBER -> BloomLevel.REMEMBER;
            case LevelOfBloomsTaxonomy.APPLY -> BloomLevel.APPLY;
            case LevelOfBloomsTaxonomy.ANALYZE -> BloomLevel.ANALYZE;
            case LevelOfBloomsTaxonomy.EVALUATE -> BloomLevel.EVALUATE;
            case LevelOfBloomsTaxonomy.CREATE -> BloomLevel.CREATE;
        };
    }

=======
     * Helper method which calculates the history of skill points earned by a user through repetition of a
     * given assessment.
     *
     * @param assessment The assessment to calculate the skill points for
     * @return Returns a list of AssessmentRepetition objects, each containing the timestamp of the repetition and
     * the earned skill points for that repetition.
     */
    private List<AssessmentRepetition> calculateSkillPointsOfRepetitions(final Assessment assessment) {
        final List<ProgressLogItem> log = assessment.getUserProgressData().getLog();
        final List<AssessmentRepetition> result = new ArrayList<>(log.size());

        // null if the assessment should not be repeated
        final Integer initialLearningInterval = assessment.getAssessmentMetadata().getInitialLearningInterval();

        // go over the log and for each repetition, check how many points the user earned for it
        // at the time of completion
        for (final ProgressLogItem currentLogItem : log) {
            double modifiers = 1.0f;

            // -10% for each hint used, but never less than 60%
            modifiers *= Math.max(1.0 - 0.1 * currentLogItem.getHintsUsed(), 0.6);

            modifiers *= currentLogItem.getCorrectness();

            // If this is a repeating assessment, the user will get fewer points for the first repetitions.
            // For this we now need to figure out how many repetitions of the content the student has done previously
            // to the current one (because repetitions give less points until the student reaches a certain number of
            // repetitions)
            if (initialLearningInterval != null) {
                final int countedRepetitions = determineCurrentRepetitionCount(assessment, currentLogItem);

                // multiply by the current repetition count compared to the needed repetition count
                // e.g. for repetition 2 of 3 the user will get 2/3 of the points
                modifiers *= (float) Math.min(countedRepetitions, REPETITIONS_NEEDED) / REPETITIONS_NEEDED;
            }

            // TODO: In the future, time taken to complete should also be taken into account. For this, time tracking
            // in the frontend and a time limit property in the assessment metadata will be necessary

            result.add(new AssessmentRepetition(
                    currentLogItem.getTimestamp(),
                    modifiers * assessment.getAssessmentMetadata().getSkillPoints()));
        }

        return result;
    }

    /**
     * Helper method which determines for a given log item of an assessment how many repetitions the user has done
     * up to that point (including the current one). Note that repetitions are only counted if they are far enough
     * apart in time. The necessary time interval is dependent on the initialLearningInterval property of the
     * assessment, the second repetition will need to be at least initialLearningInterval days after the first, and
     * for each further repetition the necessary learning interval will continue to double.
     *
     * @param assessment     The assessment the currentLogItem is located in
     * @param currentLogItem The current log item up to which the number of repetitions should be determined
     * @return Returns an integer containing the number of "valid" repetitions the user has done up to the
     * current log item.
     */
    private static int determineCurrentRepetitionCount(final Assessment assessment, final ProgressLogItem currentLogItem) {
        final List<ProgressLogItem> log = assessment.getUserProgressData().getLog();

        int countedRepetitions = 0;
        OffsetDateTime lastCountedRepetitionTime = null;
        // loop over the log items up to the current one
        for (final ProgressLogItem logItem : log) {
            if (lastCountedRepetitionTime == null) {
                // for the first time the content was done, we don't need to check the time gap
                lastCountedRepetitionTime = logItem.getTimestamp();
                countedRepetitions++;
            } else {
                // learning interval is doubled for each repetition
                final Duration minimumLearningInterval = Duration.ofDays(
                        (long) (assessment.getAssessmentMetadata().getInitialLearningInterval() * Math.pow(2, countedRepetitions - 1.0))
                );

                // if the time gap between the current log item and the previous one is large enough, count it
                // as a repetition
                if (logItem.getTimestamp().isAfter(lastCountedRepetitionTime.plus(minimumLearningInterval))) {
                    lastCountedRepetitionTime = logItem.getTimestamp();
                    countedRepetitions++;
                }
            }

            // stop iterating over the log when we have reached the current log item
            if (logItem == currentLogItem) {
                break;
            }
        }

        return countedRepetitions;
    }

    /**
     * Helper method which sums up all skill points of the passed assessments.
     *
     * @param assessments The assessments to get the skill points from.
     * @return Returns an AllSkillPoints object containing float values for each skill type containing the sum of all
     * skill points of the passed assessments for that type.
     */
    private static AllSkillPoints getTotalSkillPointsOfAssessments(final List<Assessment> assessments) {
        final AllSkillPoints skillPoints = new AllSkillPoints();

        for (final Assessment assessment : assessments) {
            for (final SkillType skillType : assessment.getAssessmentMetadata().getSkillTypes()) {
                final int value = assessment.getAssessmentMetadata().getSkillPoints();
                switch (skillType) {
                    case REMEMBER -> skillPoints.remember += value;
                    case UNDERSTAND -> skillPoints.understand += value;
                    case APPLY -> skillPoints.apply += value;
                    case ANALYSE -> skillPoints.analyze += value;
                }
            }
        }
        return skillPoints;
    }

    /**
         * Helper class which stores the timestamp and the earned skill points of a repetition of an assessment during
         * the calculation.
         */
        private record AssessmentRepetition(OffsetDateTime timestamp, double earnedSkillPoints) {
    }

    /**
     * Helper class which can be used to track skill points of all types. Should not be used outside of this
     * class because it has no encapsulation. Only used inside this parent class to make calculation methods more tidy.
     */
    private static class AllSkillPoints {
        private double remember = 0;
        private double understand = 0;
        private double apply = 0;
        private double analyze = 0;

        public double getValueBySkillType(final SkillType skillType) {
            return switch (skillType) {
                case UNDERSTAND -> understand;
                case REMEMBER -> remember;
                case APPLY -> apply;
                case ANALYSE -> analyze;
            };
        }
    }
>>>>>>> main
}
