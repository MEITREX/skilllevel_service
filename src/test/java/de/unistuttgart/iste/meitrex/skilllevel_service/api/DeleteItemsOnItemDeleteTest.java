package de.unistuttgart.iste.meitrex.skilllevel_service.api;

import de.unistuttgart.iste.meitrex.common.event.CrudOperation;
import de.unistuttgart.iste.meitrex.common.event.ItemChangeEvent;
import de.unistuttgart.iste.meitrex.skilllevel_service.controller.SubscriptionController;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.ItemDifficultyEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.SkillLevelService;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import io.dapr.client.domain.CloudEvent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels","item_difficulty","bloom_level_ability","skill_ability"})
@GraphQlApiTest
public class DeleteItemsOnItemDeleteTest {
    @Autowired
    private AllSkillLevelsRepository repository;
    @Autowired
    private ItemDifficultyRepository difficultyRepository;
    @Autowired
    private SkillLevelService skillLevelService;
    @Autowired
    private SubscriptionController subscriptionController;
    @Test
    @Transactional
    @Commit
    void testDeleteItemsOnChapterDelete() {
        // firstly, initialize the skill levels for some chapter
        final UUID userId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        ItemDifficultyEntity entity=new ItemDifficultyEntity();
        entity.setItemId(itemId);
        entity.setDifficulty(0.45f);
        entity.setNumberOfPreviousAttempts(5);
        difficultyRepository.save(entity);
        // test that the skill levels were stored in the database
        assertThat(difficultyRepository.findAll()).isNotEmpty();

        // then, call the subscription controller as if a chapter delete event was received
        subscriptionController.onItemChanged(
                new CloudEvent<>(null, null, null, null, null,
                        new ItemChangeEvent(itemId, CrudOperation.DELETE))).block();

        // now assert that the skill levels and children were deleted from the database
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    @Commit
    void testDoNotDeleteSkillLevelsOnChapterUpdate() {
        // firstly, initialize the skill levels for some chapter
        final UUID userId = UUID.randomUUID();
        final UUID itemId = UUID.randomUUID();
        ItemDifficultyEntity entity=new ItemDifficultyEntity();
        entity.setItemId(itemId);
        entity.setDifficulty(0.45f);
        entity.setNumberOfPreviousAttempts(5);
        difficultyRepository.save(entity);
        // test that the skill levels were stored in the database
        assertThat(difficultyRepository.findAll()).isNotEmpty();

        // then, call the subscription controller as if a chapter delete event was received
        subscriptionController.onItemChanged(
                new CloudEvent<>(null, null, null, null, null,
                        new ItemChangeEvent(itemId, CrudOperation.UPDATE))).block();

        // now assert that the skill levels and children were deleted from the database
        assertThat(difficultyRepository.findAll()).isNotEmpty();
    }

}
