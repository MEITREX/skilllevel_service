package de.unistuttgart.iste.gits.skilllevel_service.controller;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.SkillLevels;
import de.unistuttgart.iste.gits.skilllevel_service.service.ContentServiceClient;
import de.unistuttgart.iste.gits.skilllevel_service.service.SkillLevelService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {
    private final SkillLevelService skillLevelService;
    private final ContentServiceClient contentServiceClient;

    @Topic(name = "user-progress-updated", pubsubName = "gits")
    @PostMapping(path = "/skilllevel-service/user-progress-pubsub")
    public Mono<SkillLevels> onUserProgress(@RequestBody CloudEvent<UserProgressLogEvent> cloudEvent,
                                            @RequestHeader Map<String, String> headers) {
        log.info("Received event: {}", cloudEvent.getData());

        return Mono.fromCallable(() -> {
            try {
                UUID chapterId = contentServiceClient.getChapterIdOfContent(
                        contentServiceClient.getChapterIdOfContent(cloudEvent.getData().getContentId()));
                return skillLevelService.recalculateLevels(chapterId, cloudEvent.getData().getUserId());
            } catch (Exception e) {
                log.error("Error while processing user progress event", e);
                return null;
            }
        });
    }
}
