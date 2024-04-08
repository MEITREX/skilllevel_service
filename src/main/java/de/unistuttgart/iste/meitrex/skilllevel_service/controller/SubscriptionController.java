package de.unistuttgart.iste.meitrex.skilllevel_service.controller;

import de.unistuttgart.iste.meitrex.common.event.CourseChangeEvent;
import de.unistuttgart.iste.meitrex.common.event.CrudOperation;
import de.unistuttgart.iste.meitrex.common.event.UserProgressUpdatedEvent;
import de.unistuttgart.iste.meitrex.common.event.ItemChangeEvent;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.SkillLevelService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
                skillLevelService.recalculateLevels( cloudEvent.getData().getUserId(),cloudEvent.getData().getResponses());
            } catch (Exception e) {
                // we need to catch all exceptions because otherwise if some invalid data is in the message queue
                // it will never get processed and instead the service will just crash forever
                log.error("Error while processing user progress event", e);
            }
        });
    }

    /**
     * Dapr topic subscription to delete the stored skill level data of users for a specific course when the course
     * is deleted.
     */
    @Topic(name = "course-changed", pubsubName = "gits")
    @PostMapping(path = "/skilllevel-service/course-changes-pubsub")
    public Mono<Void> onCourseChanged(@RequestBody final CloudEvent<CourseChangeEvent> cloudEvent) {
        return Mono.fromRunnable(() -> {
            try {
                if (cloudEvent.getData().getOperation() != CrudOperation.DELETE)
                    return;
                skillLevelService.deleteSkillLevelsForCourse(cloudEvent.getData().getCourseId());
            } catch (final Exception e) {
                // we need to catch all exceptions because otherwise if some invalid data is in the message queue
                // it will never get processed and instead the service will just crash forever
                log.error("Error while processing course change event", e);
            }
        });
    }
    /**
     * Dapr topic subscription to delete the stored item data of users when the corresponding item was deleted
     */
    @Topic(name="item-changed",pubsubName = "gits")
    @PostMapping(path="/skilllevel-service/item-changed-pubsub")
    public Mono<Void> onItemChanged(@RequestBody final CloudEvent<ItemChangeEvent> cloudEvent){
        return Mono.fromRunnable(() -> {
            try {
                if (cloudEvent.getData().getOperation() != CrudOperation.DELETE)
                    return;
                skillLevelService.deleteItemDifficulty(cloudEvent.getData().getItemId());
            } catch (final Exception e) {
                // we need to catch all exceptions because otherwise if some invalid data is in the message queue
                // it will never get processed and instead the service will just crash forever
                log.error("Error while processing item change event", e);
            }
        });
    }
}
