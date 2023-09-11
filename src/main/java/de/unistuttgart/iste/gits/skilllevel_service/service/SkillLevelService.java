package de.unistuttgart.iste.gits.skilllevel_service.service;

import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.mapper.SkillLevelMapper;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.gits.skilllevel_service.service.calculation.*;
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

    /**
     * Recalculates the skill levels for a given user, course, and chapter.
     *
     * @param chapterId  the id of the chapter
     * @param userId   the id of the user
     * @return the recalculated reward scores
     */
    public SkillLevels recalculateLevels(UUID chapterId, UUID userId) {
        try {
            AllSkillLevelsEntity entity = getOrInitializeSkillLevelEntitiesForChapters(List.of(chapterId), userId).get(0);

            skillLevelCalculator.recalculateLevels(chapterId, userId, entity);

            AllSkillLevelsEntity result = skillLevelsRepository.save(entity);

            return mapper.entityToDto(result);
        } catch (Exception e) {
            throw new SkillLevelCalculationException("Could not recalculate skill levels", e);
        }
    }

    /**
     * Returns the skill levels for a given user and a list of chapters.
     * @param chapterIds The ids of the chapters to get the skill levels for
     * @param userId The id of the user to get the skill levels for
     * @return A list containing the skill levels for the given chapters in the same order as the chapterIds list
     */
    public List<SkillLevels> getSkillLevelsForChapters(List<UUID> chapterIds, UUID userId) {
        return getOrInitializeSkillLevelEntitiesForChapters(chapterIds, userId).stream().map(mapper::entityToDto).toList();
    }

    /**
     * Returns the skill levels for a given user and a list of chapters. If the skill levels for a chapter don't exist
     * yet, they will be initialized in the database with a value of 0.
     * @param chapterIds The ids of the chapters to get the skill levels for
     * @param userId The id of the user to get the skill levels for
     * @return A list containing the skill levels for the given chapters in the same order as the chapterIds list
     */
    private List<AllSkillLevelsEntity> getOrInitializeSkillLevelEntitiesForChapters(List<UUID> chapterIds,
                                                                                    UUID userId) {
        List<AllSkillLevelsEntity.PrimaryKey> primaryKeys
                = chapterIds.stream().map(x -> new AllSkillLevelsEntity.PrimaryKey(x, userId)).toList();

        // try to get the entities for the chapters
        List<AllSkillLevelsEntity> entities = skillLevelsRepository.findAllById(primaryKeys);

        // if an entity was found of every chapter, we're done
        if(entities.size() == chapterIds.size())
            return entities;

        // the list might not contain an entity for every chapter if that entity hasn't been created yet. Let's find
        // the entities that are still missing
        List<UUID> chapterIdsWithMissingEntities = new ArrayList<>(chapterIds);
        chapterIdsWithMissingEntities.removeIf(x -> entities.stream().anyMatch(y -> y.getId().getChapterId().equals(x)));

        // create the missing entities
        List<AllSkillLevelsEntity> createdEntities = new ArrayList<>();
        for(UUID chapterId : chapterIdsWithMissingEntities) {
            AllSkillLevelsEntity newEntity = new AllSkillLevelsEntity();
            newEntity.setId(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId));
            newEntity.setRemember(initializeSkillLevelEntity(0));
            newEntity.setUnderstand(initializeSkillLevelEntity(0));
            newEntity.setApply(initializeSkillLevelEntity(0));
            newEntity.setAnalyze(initializeSkillLevelEntity(0));

            // store in the db
            createdEntities.add(skillLevelsRepository.save(newEntity));
        }

        // combine the entities that were found with the newly created ones in the order of the chapterIds list
        return chapterIds.stream()
                .map(chapterId -> {
                    Optional<AllSkillLevelsEntity> entity = entities.stream()
                            .filter(x -> x.getId().getChapterId().equals(chapterId))
                            .findFirst();

                    return entity.orElseGet(() -> createdEntities.stream()
                            .filter(x -> x.getId().getChapterId().equals(chapterId))
                            .findFirst()
                            .orElseThrow());
                }).toList();
    }

    /**
     * Initializes a skill level entity with the given initial value.
     * @param initialValue The initial value to set the skill level to
     * @return The initialized skill level entity
     */
    private SkillLevelEntity initializeSkillLevelEntity(int initialValue) {
        SkillLevelEntity skillLevelEntity = new SkillLevelEntity();
        skillLevelEntity.setValue(initialValue);
        skillLevelEntity.setLog(new ArrayList<>());
        return skillLevelEntity;
    }

    /**
     * Deletes all skill levels for a given chapter.
     * @param chapterId The id of the chapter to delete the skill levels for
     */
    public void deleteSkillLevelsForChapter(UUID chapterId) {
        List<AllSkillLevelsEntity> entities = skillLevelsRepository.findByIdChapterId(chapterId);
        skillLevelsRepository.deleteAll(entities);
    }
}