package de.unistuttgart.iste.meitrex.skilllevel_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.GlobalPermissionAccessValidator;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.SkillLevels;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.SkillLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SkillLevelController {

    private final SkillLevelService skilllevelService;
    public static final String INTERNAL_NOAUTH_PREFIX = "_internal_noauth_";

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "userSkillLevelsByChapterIds")
    public List<SkillLevels> userSkillLevelsByChapterIds(@Argument final List<UUID> chapterIds, @ContextValue final LoggedInUser currentUser) {
        return skilllevelService.getSkillLevelsForChapters(chapterIds, currentUser.getId());
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "skillLevelsForUserByChapterIds")
    public List<SkillLevels> skillLevelsForUserByChapterIds(@Argument final List<UUID> chapterIds, @Argument final UUID userId) {
        return skilllevelService.getSkillLevelsForChapters(chapterIds, userId);
    }

    @MutationMapping
    public SkillLevels recalculateLevels(@Argument final UUID chapterId, @Argument final UUID userId, @ContextValue final LoggedInUser currentUser) {
        GlobalPermissionAccessValidator.validateUserHasGlobalPermission(currentUser, Set.of(LoggedInUser.RealmRole.SUPER_USER));
        return skilllevelService.recalculateLevels(chapterId, userId);
    }
}
