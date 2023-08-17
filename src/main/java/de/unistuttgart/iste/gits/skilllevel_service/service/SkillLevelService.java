package de.unistuttgart.iste.gits.skilllevel_service.service;

import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;
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
        //try {
            AllSkillLevelsEntity entity = getOrInitializeSkillLevels(chapterId, userId);

            skillLevelCalculator.recalculateLevels(chapterId, userId, entity);

            AllSkillLevelsEntity result = skillLevelsRepository.save(entity);

            return mapper.entityToDto(result);
        /*} catch (Exception e) {
            throw new SkillLevelCalculationException("Could not recalculate skill levels", e);
        }*/
    }

    public SkillLevels getSkillLevels(UUID chapterId, UUID userId) {
        return mapper.entityToDto(getOrInitializeSkillLevels(chapterId, userId));
    }

    private AllSkillLevelsEntity getOrInitializeSkillLevels(UUID chapterId, UUID userId) {
        Optional<AllSkillLevelsEntity> entity = skillLevelsRepository
                .findById(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId));

        if(entity.isPresent())
            return entity.get();

        AllSkillLevelsEntity newEntity = new AllSkillLevelsEntity();
        newEntity.setId(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId));
        newEntity.setRemember(initializeSkillLevelEntity(0));
        newEntity.setUnderstand(initializeSkillLevelEntity(0));
        newEntity.setApply(initializeSkillLevelEntity(0));
        newEntity.setAnalyze(initializeSkillLevelEntity(0));

        return skillLevelsRepository.save(newEntity);
    }

    private SkillLevelEntity initializeSkillLevelEntity(int initialValue) {
        SkillLevelEntity skillLevelEntity = new SkillLevelEntity();
        skillLevelEntity.setValue(initialValue);
        skillLevelEntity.setLog(new ArrayList<>());
        return skillLevelEntity;
    }

}