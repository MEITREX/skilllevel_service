package de.unistuttgart.iste.gits.skilllevel_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.generated.dto.SkillLevels;
import de.unistuttgart.iste.gits.skilllevel_service.service.SkillLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
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
    public SkillLevels userSkillLevels(@Argument UUID chapterId, @ContextValue LoggedInUser currentUser) {
        return skilllevelService.getSkillLevels(chapterId, currentUser.getId());
    }

    @QueryMapping
    public SkillLevels skillLevelsForUser(@Argument UUID chapterId, @Argument UUID userId) {
        return skilllevelService.getSkillLevels(chapterId, userId);
    }

    @MutationMapping
    public SkillLevels recalculateLevels(@Argument UUID chapterId, @Argument UUID userId) {
        return skilllevelService.recalculateLevels(chapterId, userId);
    }


}
