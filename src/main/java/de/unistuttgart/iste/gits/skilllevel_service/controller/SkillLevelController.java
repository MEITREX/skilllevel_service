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

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SkillLevelController {

    private final SkillLevelService skilllevelService;

    @QueryMapping
    public List<SkillLevels> userSkillLevelsByChapterIds(@Argument List<UUID> chapterIds, @ContextValue LoggedInUser currentUser) {
        return skilllevelService.getSkillLevelsForChapters(chapterIds, currentUser.getId());
    }

    @QueryMapping
    public List<SkillLevels> skillLevelsForUserByChapterIds(@Argument List<UUID> chapterIds, @Argument UUID userId) {
        return skilllevelService.getSkillLevelsForChapters(chapterIds, userId);
    }

    @MutationMapping
    public SkillLevels recalculateLevels(@Argument UUID chapterId, @Argument UUID userId) {
        return skilllevelService.recalculateLevels(chapterId, userId);
    }
}
