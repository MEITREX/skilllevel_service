package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertTrue;


@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelNonexistentSkillLevelsTest {

    @Autowired
    private AllSkillLevelsRepository repository;
    @Autowired
    private SkillLevelService skillLevelService;

    @Test
    @Transactional
    @Commit
    void testGetNonexistentSkillLevels() {
        final UUID skillId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final List<SkillLevels> skillLevels = skillLevelService.getSkillLevelsForCourse(courseId, userId);
        assertTrue(skillLevels.isEmpty());
    }
}
