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
    private final CourseServiceClient courseServiceClient;
    private final ContentServiceClient contentServiceClient;

    private final SkillLevelCalculator skillLevelCalculator;

    /**
     * Recalculates the skill levels for a given user, course, and chapter.
     *
     * @param chapterId  the id of the chapter
     * @param userId   the id of the user
     * @return the recalculated reward scores
     */
    public SkillLevels recalculateLevels(UUID chapterId, UUID userId) {
        AllSkillLevelsEntity allSkillLevelsEntity = skillLevelsRepository
                .findById(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId))
                .orElseGet(() -> initializeSkillLevels(chapterId, userId));

        List<Content> contents = contentServiceClient.getContentsWithUserProgressData(userId, List.of(chapterId));

        if(contents.isEmpty()) {
            return mapper.entityToDto(allSkillLevelsEntity);
        }

        UUID courseId = courseServiceClient.getCourseIdForContent(contents.get(0).getId());
        List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);

        try {
            skillLevelCalculator.recalculateLevels(allSkillLevelsEntity, contents, chapterIds.size());
        } catch (Exception e) {
            throw new SkillLevelCalculationException("Could not recalculate skill levels", e);
        }

        var result = skillLevelsRepository.save(allSkillLevelsEntity);

        return mapper.entityToDto(result);
    }

    public AllSkillLevelsEntity getAllSkillLevelsEntity(UUID chapterId, UUID userId) {
        return skillLevelsRepository
                .findById(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId))
                .orElseGet(() -> initializeSkillLevels(chapterId, userId));
    }

    public SkillLevels getSkillLevels(UUID chapterId, UUID userId) {
        return mapper.entityToDto(getAllSkillLevelsEntity(chapterId, userId));
    }

    public AllSkillLevelsEntity initializeSkillLevels(UUID userId, UUID chapterId) {
        AllSkillLevelsEntity allSkillLevelsEntity = new AllSkillLevelsEntity();
        allSkillLevelsEntity.setId(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId));
        allSkillLevelsEntity.setRemember(initializeSkillLevelEntity(0));
        allSkillLevelsEntity.setUnderstand(initializeSkillLevelEntity(0));
        allSkillLevelsEntity.setApply(initializeSkillLevelEntity(0));
        allSkillLevelsEntity.setAnalyze(initializeSkillLevelEntity(0));

        return skillLevelsRepository.save(allSkillLevelsEntity);
    }

    public SkillLevelEntity initializeSkillLevelEntity(int initialValue) {
        SkillLevelEntity skillLevelEntity = new SkillLevelEntity();
        skillLevelEntity.setValue(initialValue);
        skillLevelEntity.setLog(new ArrayList<>());
        return skillLevelEntity;
    }

}