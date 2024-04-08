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

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "userSkillLevelsByCourseId")
    public List<SkillLevels> userSkillLevelsByCourseId(@Argument final UUID courseId,@ContextValue final LoggedInUser currentUser) {
        return skilllevelService.getSkillLevelsForCourse(courseId, currentUser.getId());
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "skillLevelsForUserByCourseIds")
    public List<SkillLevels> skillLevelsForUserByCourseId(@Argument final UUID courseId, @Argument final UUID userId) {
        return skilllevelService.getSkillLevelsForCourse(courseId, userId);
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "usersSkillLevelBySkillIds")
    public List<SkillLevels> usersSkillLevelBySkillIds(@Argument final List<UUID> skillIds, @ContextValue final LoggedInUser currentUser) {
        return skilllevelService.getSkillLevelsForSkillIds(skillIds, currentUser.getId());
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "skillLevelForUserBySkillIds")
    public List<SkillLevels> skillLevelForUserBySkillIds(@Argument final List<UUID> skillIds, @Argument final UUID userId) {
        return skilllevelService.getSkillLevelsForSkillIds(skillIds, userId);
    }


}
