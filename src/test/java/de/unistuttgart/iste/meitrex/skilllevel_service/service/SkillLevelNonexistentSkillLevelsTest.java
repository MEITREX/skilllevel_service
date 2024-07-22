package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
<<<<<<< HEAD
=======
import de.unistuttgart.iste.meitrex.skilllevel_service.test_util.MockContentServiceClientConfiguration;
>>>>>>> main
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
<<<<<<< HEAD
=======
import org.springframework.test.context.ContextConfiguration;
>>>>>>> main

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
<<<<<<< HEAD
import static org.junit.Assert.assertTrue;


=======

@ContextConfiguration(classes = MockContentServiceClientConfiguration.class)
>>>>>>> main
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
<<<<<<< HEAD
        final UUID skillId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();
        final List<SkillLevels> skillLevels = skillLevelService.getSkillLevelsForCourse(courseId, userId);
        assertTrue(skillLevels.isEmpty());
=======
        final UUID chapterId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        final SkillLevels skillLevels = skillLevelService.getSkillLevelsForChapters(List.of(chapterId), userId).get(0);

        assertThat(skillLevels.getAnalyze().getValue()).isZero();
        assertThat(skillLevels.getApply().getValue()).isZero();
        assertThat(skillLevels.getUnderstand().getValue()).isZero();
        assertThat(skillLevels.getRemember().getValue()).isZero();

        // assert that stuff has been placed in the db correctly as well
        final AllSkillLevelsEntity entity = repository.findById(new AllSkillLevelsEntity.PrimaryKey(chapterId, userId))
                .orElseThrow();

        assertThat(entity.getId().getChapterId()).isEqualTo(chapterId);
        assertThat(entity.getId().getUserId()).isEqualTo(userId);

        assertThat(entity.getUnderstand().getValue()).isZero();
        assertThat(entity.getAnalyze().getValue()).isZero();
        assertThat(entity.getRemember().getValue()).isZero();
        assertThat(entity.getApply().getValue()).isZero();
>>>>>>> main
    }
}
