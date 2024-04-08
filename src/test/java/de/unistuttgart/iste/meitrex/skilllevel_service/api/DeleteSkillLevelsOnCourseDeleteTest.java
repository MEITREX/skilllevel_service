package de.unistuttgart.iste.meitrex.skilllevel_service.api;

import de.unistuttgart.iste.meitrex.common.event.CourseChangeEvent;
import de.unistuttgart.iste.meitrex.common.event.CrudOperation;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.skilllevel_service.controller.SubscriptionController;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillsForCourse;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillsForCourseRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.SkillLevelService;
import io.dapr.client.domain.CloudEvent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;


@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels","bloom_level_ability","skill_ability"})
@GraphQlApiTest
class DeleteSkillLevelsOnCourseDeleteTest {
    @Autowired
    private AllSkillLevelsRepository repository;
    @Autowired
    private SkillsForCourseRepository skillsForCourseRepository;
    @Autowired
    private SkillLevelService skillLevelService;
    @Autowired
    private SubscriptionController subscriptionController;
    @BeforeEach
    void clearDatabase(){
        repository.deleteAll();
    }
    @Test
    @Transactional
    @Commit
    void testDeleteSkillLevelsOnChapterDelete() {
        // firstly, initialize the skill levels for some chapter
        final UUID userId = UUID.randomUUID();
        final UUID skillId = UUID.randomUUID();
        final UUID courseId=UUID.randomUUID();
        final AllSkillLevelsEntity entity=AllSkillLevelsEntity.builder()
                .id(new AllSkillLevelsEntity.PrimaryKey(skillId,userId))
                .remember(SkillLevelEntity.builder().value(0).build())
                .understand(SkillLevelEntity.builder().value(0).build())
                .analyze(SkillLevelEntity.builder().value(0).build())
                .apply(SkillLevelEntity.builder().value(0).build())
                .evaluate(SkillLevelEntity.builder().value(0).build())
                .create(SkillLevelEntity.builder().value(0).build())
                .build();
        repository.save(entity);
        SkillsForCourse course=SkillsForCourse.builder()
                .courseId(courseId)
                .skillId(skillId)
                .id(UUID.randomUUID())
                .build();
        skillsForCourseRepository.save(course);
        skillLevelService.getSkillLevelsForCourse(courseId, userId);

        // test that the skill levels were stored in the database
        assertThat(repository.findAll()).isNotEmpty();

        // then, call the subscription controller as if a chapter delete event was received
        subscriptionController.onCourseChanged(
                new CloudEvent<>(null, null, null, null, null,
                        new CourseChangeEvent(courseId, CrudOperation.DELETE))).block();

        // now assert that the skill levels and children were deleted from the database
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    @Commit
    void testDoNotDeleteSkillLevelsOnChapterUpdate() {
        // firstly, initialize the skill levels for some chapter
        final UUID userId = UUID.randomUUID();
        final UUID skillId = UUID.randomUUID();
        final UUID courseId=UUID.randomUUID();
        final AllSkillLevelsEntity entity=AllSkillLevelsEntity.builder()
                .id(new AllSkillLevelsEntity.PrimaryKey(skillId,userId))
                .remember(SkillLevelEntity.builder().value(0).build())
                .understand(SkillLevelEntity.builder().value(0).build())
                .analyze(SkillLevelEntity.builder().value(0).build())
                .apply(SkillLevelEntity.builder().value(0).build())
                .evaluate(SkillLevelEntity.builder().value(0).build())
                .create(SkillLevelEntity.builder().value(0).build())
                .build();
        repository.save(entity);
        // test that the skill levels were stored in the database
        assertThat(repository.findAll()).isNotEmpty();

        // then, call the subscription controller as if a chapter delete event was received
        subscriptionController.onCourseChanged(
                new CloudEvent<>(null, null, null, null, null,
                        new CourseChangeEvent(courseId, CrudOperation.UPDATE))).block();

        // now assert that the skill levels and children were deleted from the database
        assertThat(repository.findAll()).isNotEmpty();
    }
}
