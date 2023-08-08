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

    private final RememberLevelCalculator rememberLevelCalculator;
    private final UnderstandlevelCalculator understandlevelCalculator;
    private final ApplyLevelCalculator applyLevelCalculator;
    private final AnalyzeLevelCalculator analyzeLevelCalculator;


    /**
     * Recalculates the skill level for a given user and course.
     *
     * @param courseId  the id of the course
     * @param userId    the id of the user
     * @param chapterId the id of the chapter
     * @return the recalculated skill levels
     */
    public SkillLevels recalculateSkills(UUID courseId, UUID userId, UUID chapterId) {
        AllSkillLevelsEntity allSkillLevelsEntity = skillLevelsRepository
                .findById(new AllSkillLevelsEntity.PrimaryKey(courseId, userId, chapterId))
                .orElseGet(() -> initializeSkillLevels(courseId, userId, chapterId));

        List<UUID> chapterIds = courseServiceClient.getChapterIds(courseId);

        List<Content> contents = contentServiceClient.getContentsWithUserProgressData(userId, chapterIds);

        allSkillLevelsEntity
                .setRemember(rememberLevelCalculator.recalculateLevel(allSkillLevelsEntity.getRemember(), contents));
        allSkillLevelsEntity
                .setUnderstand(understandlevelCalculator.recalculateLevel(allSkillLevelsEntity.getUnderstand(), contents));
        allSkillLevelsEntity
                .setApply(applyLevelCalculator.recalculateLevel(allSkillLevelsEntity.getApply(), contents));
        allSkillLevelsEntity
                .setAnalyze(analyzeLevelCalculator.recalculateLevel(allSkillLevelsEntity.getAnalyze(), contents));


        var result = skillLevelsRepository.save(allSkillLevelsEntity);

        return mapper.entityToDto(result);
    }

    public AllSkillLevelsEntity getAllSkillLevelsEntity(UUID courseId, UUID userId, UUID chapterId) {
        return skillLevelsRepository
                .findById(new AllSkillLevelsEntity.PrimaryKey(courseId, userId, chapterId))
                .orElseGet(() -> initializeSkillLevels(courseId, userId, chapterId));
    }

    public SkillLevels getSkillLevels(UUID courseId, UUID userId, UUID chapterId) {
        return mapper.entityToDto(getAllSkillLevelsEntity(courseId, userId, chapterId));
    }

    public AllSkillLevelsEntity initializeSkillLevels(UUID courseId, UUID userId, UUID chapterId) {
        AllSkillLevelsEntity allSkillLevelsEntity = new AllSkillLevelsEntity();
        allSkillLevelsEntity.setId(new AllSkillLevelsEntity.PrimaryKey(courseId, userId, chapterId));
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