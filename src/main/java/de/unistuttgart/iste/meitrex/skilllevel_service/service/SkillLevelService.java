package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.common.event.ItemResponse;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAbilityEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillsForCourse;
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

    /**
     * Recalculates the skill levels for a given user and responses.
     *
     * @param userId    the id of the user
     * @param responses a list with responses
     * @return the recalculated reward scores
     */
    public void recalculateLevels( final UUID userId,final List<ItemResponse>responses) {
        try {
            log.info("Recalculating skill levels.");
            skillLevelCalculator.recalculateLevels(userId, responses);

        } catch (final Exception e) {
            throw new SkillLevelCalculationException("Could not recalculate skill levels", e);
        }
    }

    /**
     * return the skill levels, that belong to the given user and course
     * @param courseId the id of the course
     * @param userId the id of the user
     * @return skill levels of the user for the given course
     */
    public List<SkillLevels> getSkillLevelsForCourse(UUID courseId,UUID userId){
        return getSkillLevelEntitiesForCourse(courseId, userId).stream().map(mapper::entityToDto).toList();

    }

    /**
     * return the skill levels of the given user and the given skills
     * @param skillIds the ids of the skills
     * @param userId the id of the user
     * @return the skill levels for the given user and skills
     */
    public List<SkillLevels> getSkillLevelsForSkillIds(List<UUID> skillIds,UUID userId){
        return getSkillLevelEntitiesForSkillIds(skillIds, userId).stream().map(mapper::entityToDto).toList();

    }

    /**
     * Returns the skill levels for a given user and a course.
     *
     * @param courseId The ids of the course to get the skill levels for
     * @param userId     The id of the user to get the skill levels for
     * @return A list containing the skill levels for the given course
     */
    private List<AllSkillLevelsEntity> getSkillLevelEntitiesForCourse(final UUID courseId,
                                                                                    final UUID userId) {
        List< SkillsForCourse> skills=skillsForCourseRepository.findByCourseId(courseId);
        List<UUID>skillIds=new ArrayList<>();
        for(SkillsForCourse skillsForCourse:skills){
            skillIds.add(skillsForCourse.getSkillId());
        }
        return getSkillLevelEntitiesForSkillIds(skillIds,userId);

    }


    /**
     * Returns the skill levels for a given user and skills.
     *
     * @param skillIds The ids of the skills to get the skill levels for
     * @param userId     The id of the user to get the skill levels for
     * @return A list containing the skill levels for the given course
     */
    private List<AllSkillLevelsEntity> getSkillLevelEntitiesForSkillIds(final List<UUID>skillIds,
                                                                      final UUID userId) {
        final List<AllSkillLevelsEntity.PrimaryKey> primaryKeys = skillIds.stream().map(x -> new AllSkillLevelsEntity.PrimaryKey(x, userId)).toList();
        final List<AllSkillLevelsEntity> entities = skillLevelsRepository.findAllById(primaryKeys);
        return entities;
    }





    /**
     * Deletes all skill levels for a given course. As well as all skill abilities for the associated skill abilities
     *
     * @param courseId The id of the course to delete the skill levels for
     */
    public void deleteSkillLevelsForCourse(final UUID courseId){
        final List<SkillsForCourse> skills=skillsForCourseRepository.findByCourseId(courseId);

        //get all SkillIds and delete the corresponding abilities
        HashSet<UUID> skillIds=new HashSet<>();
        HashSet<UUID> userIds=new HashSet<>();
        for(SkillsForCourse skill:skills){
            skillIds.add(skill.getSkillId());
        }
        for(UUID skillId:skillIds){
            List<AllSkillLevelsEntity>abilitiesForSkillId=skillLevelsRepository.findByIdSkillId(skillId);
            for(AllSkillLevelsEntity ability:abilitiesForSkillId){
                skillAbilityRepository.deleteById(new SkillAbilityEntity.PrimaryKey(skillId,ability.getId().getUserId()));
                skillLevelsRepository.deleteById(new AllSkillLevelsEntity.PrimaryKey(skillId,ability.getId().getUserId()));
            }
        }
    }

    /**
     * Deletes the item difficulty for the given item id
     *
     * @param itemId The id of the item whose difficulty should be deleted
     */
    public void deleteItemDifficulty(final UUID itemId){
        itemDifficultyRepository.deleteById(itemId);
    }
}