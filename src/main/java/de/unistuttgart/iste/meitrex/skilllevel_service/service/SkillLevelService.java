package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.skilllevels.UserSkillLevelChangedEvent;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAbilityEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillsForCourse;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAverageValueEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.mapper.SkillLevelMapper;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillAbilityRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillsForCourseRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.calculation.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SkillLevelService {

    private final AllSkillLevelsRepository skillLevelsRepository;
    private final SkillLevelMapper mapper;
    private final SkillLevelCalculator skillLevelCalculator;

    private final SkillAbilityRepository skillAbilityRepository;

    private final ItemDifficultyRepository itemDifficultyRepository;

    private final SkillsForCourseRepository skillsForCourseRepository;
    
    private final TopicPublisher topicPublisher;

    /**
     * Recalculates the skill levels for a given user and responses.
     *
     * @param userId    the id of the user
     * @param responses a list with responses
     * @return the recalculated reward scores
     */
    public void recalculateLevels(final UUID userId, final List<ItemResponse> responses) {
        try {
            log.info("Recalculating skill levels.");
            skillLevelCalculator.recalculateLevels(userId, responses);

        } catch (final Exception e) {
            throw new SkillLevelCalculationException("Could not recalculate skill levels", e);
        }
    }

    /**
     * return the skill levels, that belong to the given user and course
     *
     * @param courseId the id of the course
     * @param userId   the id of the user
     * @return skill levels of the user for the given course
     */
    public List<SkillLevels> getSkillLevelsForCourse(UUID courseId, UUID userId) {
        return getSkillLevelEntitiesForCourse(courseId, userId).stream().map(mapper::entityToDto).toList();

    }

    /**
     * return the skill levels of the given user and the given skills
     *
     * @param skillIds the ids of the skills
     * @param userId   the id of the user
     * @return the skill levels for the given user and skills
     */
    public List<SkillLevels> getSkillLevelsForSkillIds(List<UUID> skillIds, UUID userId) {
        return getSkillLevelEntitiesForSkillIds(skillIds, userId).stream().map(mapper::entityToDto).toList();
    }

    /**
     * Returns the skill average values for given skillIds.
     *
     * @param skillIds List of skillIds
     * @return List of SkillAverageValueEntity that represents the average values of the given skills
     */
    public List<SkillAverageValueEntity> getAverageSkillValuesForSkillIds(List<UUID> skillIds) {
        List<SkillAverageValueEntity> skillAverageValues = new ArrayList<>();
        for(UUID skillId : skillIds) {
            skillAverageValues.add(getAverageSkillValueForSkillId(skillId));
        }
        return skillAverageValues;
    }

    /**
     * Returns the skill average value for a given skillId.
     *
     * @param skillId skillId of skillLevels
     * @return SkillAverageValueEntity that represents the average value of a specific skill
     */
    private SkillAverageValueEntity getAverageSkillValueForSkillId(UUID skillId) {
        List<AllSkillLevelsEntity> skillLevels = skillLevelsRepository.findByIdSkillId(skillId);
        List<Float> skillValues = new ArrayList<>();
        for(AllSkillLevelsEntity skillLevel : skillLevels) {
            skillValues.add(getSkillValueForSkillLevel(skillLevel));
        }
        float sum = 0f;
        for(Float skillValue : skillValues) {
            sum += skillValue;
        }
        SkillAverageValueEntity skillAverageValue = new SkillAverageValueEntity();
        skillAverageValue.setSkillId(skillId);
        skillAverageValue.setParticipantCount(skillValues.size());
        if (skillValues.isEmpty()) {
            skillAverageValue.setAverageValue(0f);
        } 
        else {
            skillAverageValue.setAverageValue(sum / skillValues.size());
        }
        return skillAverageValue;
    }

    /**
     * Returns the skill value for a given skillLevel by combining all 6 skillLevelEntities.
     *
     * @param skillLevel skillLevel of a skill
     * @return A float value that represents the skill value of a skill
     */
    private Float getSkillValueForSkillLevel(AllSkillLevelsEntity skillLevel) {
        List<Float> values = List.of(
            skillLevel.getRemember().getValue(),
            skillLevel.getUnderstand().getValue(),
            skillLevel.getApply().getValue(),
            skillLevel.getAnalyze().getValue(),
            skillLevel.getEvaluate().getValue(),
            skillLevel.getCreate().getValue()
        );
        List<Float> nonZeroValues = values.stream().filter(v -> v > 0f).toList();
        if (nonZeroValues.isEmpty()) {
            return 0f;
        }
        else{
            float sum = nonZeroValues.stream().reduce(0f, Float::sum);
            return sum / nonZeroValues.size();
        }
    }

    /**
     * return the skill value of the given user and the given skill
     *
     * @param skillId the ids of the skill
     * @param userId   the id of the user
     * @return the skill value for the given user and skill
     */
    public Float getSkillValueForSkillId(UUID skillId, UUID userId) {
        List<AllSkillLevelsEntity> skillLevels = getSkillLevelEntitiesForSkillIds(List.of(skillId), userId);
        return getSkillValueForSkillLevel(skillLevels.get(0));
    }

    /**
     * Returns the skill levels for a given user and a course.
     *
     * @param courseId The ids of the course to get the skill levels for
     * @param userId   The id of the user to get the skill levels for
     * @return A list containing the skill levels for the given course
     */
    private List<AllSkillLevelsEntity> getSkillLevelEntitiesForCourse(final UUID courseId,
                                                                      final UUID userId) {
        List<SkillsForCourse> skills = skillsForCourseRepository.findByCourseId(courseId);
        List<UUID> skillIds = new ArrayList<>();
        for (SkillsForCourse skillsForCourse : skills) {
            skillIds.add(skillsForCourse.getSkillId());
        }
        return getSkillLevelEntitiesForSkillIds(skillIds, userId);

    }


    /**
     * Returns the skill levels for a given user and skills.
     *
     * @param skillIds The ids of the skills to get the skill levels for
     * @param userId   The id of the user to get the skill levels for
     * @return A list containing the skill levels for the given course
     */
    private List<AllSkillLevelsEntity> getSkillLevelEntitiesForSkillIds(final List<UUID> skillIds,
                                                                        final UUID userId) {
        final List<AllSkillLevelsEntity.PrimaryKey> primaryKeys = skillIds.stream().map(x -> new AllSkillLevelsEntity.PrimaryKey(x, userId)).toList();
        final List<AllSkillLevelsEntity> entities = skillLevelsRepository.findAllById(primaryKeys);
        List<AllSkillLevelsEntity> newSkillLevels = new ArrayList<>();
        for (UUID skillId : skillIds) {
            boolean found = false;
            for (AllSkillLevelsEntity entity : entities) {
                if (entity.getId().getSkillId().equals(skillId)) {
                    found = true;
                    newSkillLevels.add(entity);
                    break;
                }
            }
            if (!found) {
                AllSkillLevelsEntity newEntity = new AllSkillLevelsEntity();
                newEntity.setId(new AllSkillLevelsEntity.PrimaryKey(skillId, userId));
                newEntity.setRemember(initializeSkillLevelEntity(0));
                newEntity.setUnderstand(initializeSkillLevelEntity(0));
                newEntity.setApply(initializeSkillLevelEntity(0));
                newEntity.setAnalyze(initializeSkillLevelEntity(0));
                newEntity.setEvaluate(initializeSkillLevelEntity(0));
                newEntity.setCreate(initializeSkillLevelEntity(0));
                newSkillLevels.add(newEntity);
            }
        }
        return newSkillLevels;
    }


    /**
     * Deletes all skill levels for a given course. As well as all skill abilities for the associated skill abilities
     *
     * @param courseId The id of the course to delete the skill levels for
     */
    public void deleteSkillLevelsForCourse(final UUID courseId) {
        final List<SkillsForCourse> skills = skillsForCourseRepository.findByCourseId(courseId);

        //get all SkillIds and delete the corresponding abilities
        HashSet<UUID> skillIds = new HashSet<>();
        HashSet<UUID> userIds = new HashSet<>();
        for (SkillsForCourse skill : skills) {
            skillIds.add(skill.getSkillId());
        }
        for (UUID skillId : skillIds) {
            List<AllSkillLevelsEntity> abilitiesForSkillId = skillLevelsRepository.findByIdSkillId(skillId);
            for (AllSkillLevelsEntity ability : abilitiesForSkillId) {
                skillAbilityRepository.deleteById(new SkillAbilityEntity.PrimaryKey(skillId, ability.getId().getUserId()));
                skillLevelsRepository.deleteById(new AllSkillLevelsEntity.PrimaryKey(skillId, ability.getId().getUserId()));
            }
        }
    }

    /**
     * Deletes the item difficulty for the given item id
     *
     * @param itemId The id of the item whose difficulty should be deleted
     */
    public void deleteItemDifficulty(final UUID itemId) {
        itemDifficultyRepository.deleteById(itemId);
    }

    private SkillLevelEntity initializeSkillLevelEntity(final float initialValue) {
        final SkillLevelEntity skillLevelEntity = new SkillLevelEntity(initialValue);
        skillLevelEntity.setValue(initialValue);
        skillLevelEntity.setLog(new ArrayList<>());
        return skillLevelEntity;
    }

    /**
     * Publishes UserSkillLevelChangedEvent for all skill levels of a given user.
     * This is used to respond to RequestUserSkillLevelEvent.
     *
     * @param userId The ID of the user to publish skill levels for
     */
    public void publishAllSkillLevelsForUser(final UUID userId) {
        List<AllSkillLevelsEntity> allSkillLevels = skillLevelsRepository.findByIdUserId(userId);
        
        for (AllSkillLevelsEntity skillLevelEntity : allSkillLevels) {
            UUID skillId = skillLevelEntity.getId().getSkillId();
            
            publishSkillLevelEvent(userId, skillId, BloomLevel.REMEMBER, skillLevelEntity.getRemember().getValue());
            publishSkillLevelEvent(userId, skillId, BloomLevel.UNDERSTAND, skillLevelEntity.getUnderstand().getValue());
            publishSkillLevelEvent(userId, skillId, BloomLevel.APPLY, skillLevelEntity.getApply().getValue());
            publishSkillLevelEvent(userId, skillId, BloomLevel.ANALYZE, skillLevelEntity.getAnalyze().getValue());
            publishSkillLevelEvent(userId, skillId, BloomLevel.EVALUATE, skillLevelEntity.getEvaluate().getValue());
            publishSkillLevelEvent(userId, skillId, BloomLevel.CREATE, skillLevelEntity.getCreate().getValue());
        }
    }

    /**
     * Method to publish a single UserSkillLevelChangedEvent.
     * oldValue and newValue are set to the same value since this is just for publishing existing skill levels and not for indicating a change
     */
    private void publishSkillLevelEvent(UUID userId, UUID skillId, BloomLevel bloomLevel, float value) {
        UserSkillLevelChangedEvent event = UserSkillLevelChangedEvent.builder()
                .userId(userId)
                .skillId(skillId)
                .bloomLevel(bloomLevel)
                .oldValue(value)
                .newValue(value)
                .build();
        
        topicPublisher.notifyUserSkillLevelChanged(event);
    }
}