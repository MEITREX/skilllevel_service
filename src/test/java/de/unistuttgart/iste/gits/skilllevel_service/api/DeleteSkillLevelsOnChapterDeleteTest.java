package de.unistuttgart.iste.gits.skilllevel_service.api;

import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.skilllevel_service.controller.SubscriptionController;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.gits.skilllevel_service.service.SkillLevelService;
import de.unistuttgart.iste.gits.skilllevel_service.test_util.MockContentServiceClientConfiguration;
import io.dapr.client.domain.CloudEvent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ContextConfiguration(classes = MockContentServiceClientConfiguration.class)
@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class DeleteSkillLevelsOnChapterDeleteTest {
    @Autowired
    private AllSkillLevelsRepository repository;
    @Autowired
    private SkillLevelService skillLevelService;
    @Autowired
    private SubscriptionController subscriptionController;

    @Test
    @Transactional
    @Commit
    void testDeleteSkillLevelsOnChapterDelete() {
        // firstly, initialize the skill levels for some chapter
        final UUID userId = UUID.randomUUID();
        final UUID chapterId = UUID.randomUUID();
        skillLevelService.getSkillLevelsForChapters(List.of(chapterId), userId).get(0);

        // test that the skill levels were stored in the database
        assertThat(repository.findAll()).isNotEmpty();

        // then, call the subscription controller as if a chapter delete event was received
        subscriptionController.onChapterChanged(
                new CloudEvent<>(null, null, null, null, null,
                        new ChapterChangeEvent(List.of(chapterId), CrudOperation.DELETE))).block();

        // now assert that the skill levels and children were deleted from the database
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    @Commit
    void testDoNotDeleteSkillLevelsOnChapterUpdate() {
        // firstly, initialize the skill levels for some chapter
        final UUID userId = UUID.randomUUID();
        final UUID chapterId = UUID.randomUUID();
        skillLevelService.getSkillLevelsForChapters(List.of(chapterId), userId).get(0);

        // test that the skill levels were stored in the database
        assertThat(repository.findAll()).isNotEmpty();

        // then, call the subscription controller as if a chapter delete event was received
        subscriptionController.onChapterChanged(
                new CloudEvent<>(null, null, null, null, null,
                        new ChapterChangeEvent(List.of(chapterId), CrudOperation.UPDATE))).block();

        // now assert that the skill levels and children were deleted from the database
        assertThat(repository.findAll()).isNotEmpty();
    }
}
