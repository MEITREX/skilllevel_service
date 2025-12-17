package de.unistuttgart.iste.meitrex.skilllevel_service.controller;

import de.unistuttgart.iste.meitrex.common.event.RequestUserSkillLevelEvent;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.SkillLevelService;
import io.dapr.client.domain.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Tests for SubscriptionController event handling.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionControllerRequestSkillLevelTest {

    @Mock
    private SkillLevelService skillLevelService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @Test
    void testOnRequestUserSkillLevel_CallsPublishAllSkillLevelsForUser() {
        UUID userId = UUID.randomUUID();
        RequestUserSkillLevelEvent event = RequestUserSkillLevelEvent.builder()
                .userId(userId)
                .build();
        
        CloudEvent<RequestUserSkillLevelEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        subscriptionController.onRequestUserSkillLevel(cloudEvent).block();

        verify(skillLevelService, times(1)).publishAllSkillLevelsForUser(userId);
    }

    @Test
    void testOnRequestUserSkillLevel_WithException_DoesNotThrow() {
        UUID userId = UUID.randomUUID();
        RequestUserSkillLevelEvent event = RequestUserSkillLevelEvent.builder()
                .userId(userId)
                .build();
        
        CloudEvent<RequestUserSkillLevelEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);
        
        doThrow(new RuntimeException("Database error"))
                .when(skillLevelService)
                .publishAllSkillLevelsForUser(userId);

        subscriptionController.onRequestUserSkillLevel(cloudEvent).block();

        verify(skillLevelService, times(1)).publishAllSkillLevelsForUser(userId);
    }

    @Test
    void testOnRequestUserSkillLevel_MultipleUsers() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        RequestUserSkillLevelEvent event1 = RequestUserSkillLevelEvent.builder().userId(userId1).build();
        RequestUserSkillLevelEvent event2 = RequestUserSkillLevelEvent.builder().userId(userId2).build();
        RequestUserSkillLevelEvent event3 = RequestUserSkillLevelEvent.builder().userId(userId3).build();

        CloudEvent<RequestUserSkillLevelEvent> cloudEvent1 = mock(CloudEvent.class);
        CloudEvent<RequestUserSkillLevelEvent> cloudEvent2 = mock(CloudEvent.class);
        CloudEvent<RequestUserSkillLevelEvent> cloudEvent3 = mock(CloudEvent.class);

        when(cloudEvent1.getData()).thenReturn(event1);
        when(cloudEvent2.getData()).thenReturn(event2);
        when(cloudEvent3.getData()).thenReturn(event3);

        subscriptionController.onRequestUserSkillLevel(cloudEvent1).block();
        subscriptionController.onRequestUserSkillLevel(cloudEvent2).block();
        subscriptionController.onRequestUserSkillLevel(cloudEvent3).block();

        verify(skillLevelService, times(1)).publishAllSkillLevelsForUser(userId1);
        verify(skillLevelService, times(1)).publishAllSkillLevelsForUser(userId2);
        verify(skillLevelService, times(1)).publishAllSkillLevelsForUser(userId3);
    }

    @Test
    void testOnRequestUserSkillLevel_WithNullEvent() {
        CloudEvent<RequestUserSkillLevelEvent> nullEventCloud = mock(CloudEvent.class);
        when(nullEventCloud.getData()).thenReturn(null);

        subscriptionController.onRequestUserSkillLevel(nullEventCloud).block();

        verify(skillLevelService, never()).publishAllSkillLevelsForUser(any());
    }
}
