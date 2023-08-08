package de.unistuttgart.iste.gits.skilllevel_service.controller;

import de.unistuttgart.iste.gits.generated.dto.SkillLevels;
import de.unistuttgart.iste.gits.skilllevel_service.service.SkillLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SkillLevelController {

    private final SkillLevelService skilllevelService;

    @QueryMapping
    public SkillLevels userSkillLevels(@Argument UUID courseId, @Argument UUID userId, @Argument UUID chapterId) {
        return skilllevelService.getSkillLevels(courseId, userId, chapterId);
    }

    @QueryMapping
    public SkillLevels skillLevelsForUser(@Argument UUID courseId, @Argument UUID userId, @Argument UUID chapterId) {
        return skilllevelService.getSkillLevels(courseId, userId, chapterId);
    }

    @MutationMapping
    public SkillLevels recalculateSkills(@Argument UUID courseId, @Argument UUID userId, @Argument UUID chapterId) {
        return skilllevelService.recalculateSkills(courseId, userId, chapterId);
    }


}
