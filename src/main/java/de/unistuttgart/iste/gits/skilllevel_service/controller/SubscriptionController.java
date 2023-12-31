package de.unistuttgart.iste.gits.skilllevel_service.controller;

import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.event.UserProgressUpdatedEvent;
import de.unistuttgart.iste.gits.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.gits.skilllevel_service.service.SkillLevelService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controller for Dapr pubsub topic subscriptions.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {
    private final SkillLevelService skillLevelService;

    /**
     * Dapr topic subscription to recalculate the skill levels of a user for a specific chapter when the user
     * completes an assessment in that chapter.
     */
    @Topic(name = "user-progress-updated", pubsubName = "gits")
    @PostMapping(path = "/skilllevel-service/user-progress-pubsub")
    public Mono<Void> onUserProgress(@RequestBody final CloudEvent<UserProgressUpdatedEvent> cloudEvent) {
        log.info("Received event: {}", cloudEvent.getData());

        return Mono.fromRunnable(() -> {
            try {
                skillLevelService.recalculateLevels(cloudEvent.getData().getChapterId(), cloudEvent.getData().getUserId());
            } catch (Exception e) {
                // we need to catch all exceptions because otherwise if some invalid data is in the message queue
                // it will never get processed and instead the service will just crash forever
                log.error("Error while processing user progress event", e);
            }
        });
    }

    /**
     * Dapr topic subscription to delete the stored skill level data of users for a specific chapter when the chapter
     * is deleted.
     */
    @Topic(name = "chapter-changed", pubsubName = "gits")
    @PostMapping(path = "/skilllevel-service/chapted-changes-pubsub")
    public Mono<Void> onChapterChanged(@RequestBody final CloudEvent<ChapterChangeEvent> cloudEvent) {
        return Mono.fromRunnable(() -> {
            try {
                if (cloudEvent.getData().getOperation() != CrudOperation.DELETE)
                    return;

                for (final UUID chapterId : cloudEvent.getData().getChapterIds()) {
                    skillLevelService.deleteSkillLevelsForChapter(chapterId);
                }
            } catch (final Exception e) {
                // we need to catch all exceptions because otherwise if some invalid data is in the message queue
                // it will never get processed and instead the service will just crash forever
                log.error("Error while processing course change event", e);
            }
        });
    }
}
