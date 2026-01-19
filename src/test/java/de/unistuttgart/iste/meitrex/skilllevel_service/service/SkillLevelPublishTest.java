package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.skilllevels.UserSkillLevelChangedEvent;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for publishing skill level events.
 */
@TablesToDelete({"skill_level_log", "skill_level_log_entry", "skill_levels"})
@GraphQlApiTest
class SkillLevelPublishTest {

    @Autowired
    private SkillLevelService skillLevelService;

    @Autowired
    private AllSkillLevelsRepository skillLevelsRepository;

    @SpyBean
    private TopicPublisher topicPublisher;

    @Test
    @Transactional
    @Commit
    void testPublishAllSkillLevelsForUser_WithExistingSkills() {
        UUID userId = UUID.randomUUID();
        UUID skillId1 = UUID.randomUUID();
        UUID skillId2 = UUID.randomUUID();

        AllSkillLevelsEntity skillLevel1 = createSkillLevelEntity(userId, skillId1, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f);
        AllSkillLevelsEntity skillLevel2 = createSkillLevelEntity(userId, skillId2, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f);

        skillLevelsRepository.save(skillLevel1);
        skillLevelsRepository.save(skillLevel2);

        skillLevelService.publishAllSkillLevelsForUser(userId);

        ArgumentCaptor<UserSkillLevelChangedEvent> eventCaptor = 
                ArgumentCaptor.forClass(UserSkillLevelChangedEvent.class);
        verify(topicPublisher, times(12)).notifyUserSkillLevelChanged(eventCaptor.capture());

        List<UserSkillLevelChangedEvent> capturedEvents = eventCaptor.getAllValues();
        assertEquals(12, capturedEvents.size());

        verifySkillLevelEvents(capturedEvents, userId, skillId1, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f);

        verifySkillLevelEvents(capturedEvents, userId, skillId2, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f);
    }

    @Test
    @Transactional
    @Commit
    void testPublishAllSkillLevelsForUser_WithNoSkills() {
        UUID userId = UUID.randomUUID();

        skillLevelService.publishAllSkillLevelsForUser(userId);

        verify(topicPublisher, never()).notifyUserSkillLevelChanged(any());
    }

    @Test
    @Transactional
    @Commit
    void testPublishAllSkillLevelsForUser_VerifyEventContent() {
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        float rememberValue = 0.75f;

        AllSkillLevelsEntity skillLevel = createSkillLevelEntity(userId, skillId, 
                rememberValue, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f);
        skillLevelsRepository.save(skillLevel);

        skillLevelService.publishAllSkillLevelsForUser(userId);

        ArgumentCaptor<UserSkillLevelChangedEvent> eventCaptor = 
                ArgumentCaptor.forClass(UserSkillLevelChangedEvent.class);
        verify(topicPublisher, times(6)).notifyUserSkillLevelChanged(eventCaptor.capture());

        List<UserSkillLevelChangedEvent> capturedEvents = eventCaptor.getAllValues();

        UserSkillLevelChangedEvent rememberEvent = capturedEvents.stream()
                .filter(e -> e.getBloomLevel() == BloomLevel.REMEMBER)
                .findFirst()
                .orElse(null);

        assertNotNull(rememberEvent);
        assertEquals(userId, rememberEvent.getUserId());
        assertEquals(skillId, rememberEvent.getSkillId());
        assertEquals(BloomLevel.REMEMBER, rememberEvent.getBloomLevel());
        assertEquals(rememberValue, rememberEvent.getOldValue(), 0.001f);
        assertEquals(rememberValue, rememberEvent.getNewValue(), 0.001f);
    }

    @Test
    @Transactional
    @Commit
    void testPublishAllSkillLevelsForUser_AllBloomLevelsPublished() {
        UUID userId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();

        AllSkillLevelsEntity skillLevel = createSkillLevelEntity(userId, skillId, 
                0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f);
        skillLevelsRepository.save(skillLevel);

        skillLevelService.publishAllSkillLevelsForUser(userId);

        ArgumentCaptor<UserSkillLevelChangedEvent> eventCaptor = 
                ArgumentCaptor.forClass(UserSkillLevelChangedEvent.class);
        verify(topicPublisher, times(6)).notifyUserSkillLevelChanged(eventCaptor.capture());

        List<UserSkillLevelChangedEvent> capturedEvents = eventCaptor.getAllValues();

        assertTrue(capturedEvents.stream().anyMatch(e -> e.getBloomLevel() == BloomLevel.REMEMBER));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getBloomLevel() == BloomLevel.UNDERSTAND));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getBloomLevel() == BloomLevel.APPLY));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getBloomLevel() == BloomLevel.ANALYZE));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getBloomLevel() == BloomLevel.EVALUATE));
        assertTrue(capturedEvents.stream().anyMatch(e -> e.getBloomLevel() == BloomLevel.CREATE));
    }

    @Test
    @Transactional
    @Commit
    void testPublishAllSkillLevelsForUser_MultipleSkills() {
        UUID userId = UUID.randomUUID();
        UUID skillId1 = UUID.randomUUID();
        UUID skillId2 = UUID.randomUUID();
        UUID skillId3 = UUID.randomUUID();

        skillLevelsRepository.save(createSkillLevelEntity(userId, skillId1, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f));
        skillLevelsRepository.save(createSkillLevelEntity(userId, skillId2, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f));
        skillLevelsRepository.save(createSkillLevelEntity(userId, skillId3, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f));

        skillLevelService.publishAllSkillLevelsForUser(userId);

        verify(topicPublisher, times(18)).notifyUserSkillLevelChanged(any());
    }

    /**
     * Helper method to create a skill level entity with specified values
     */
    private AllSkillLevelsEntity createSkillLevelEntity(UUID userId, UUID skillId, 
            float remember, float understand, float apply, 
            float analyze, float evaluate, float create) {
        AllSkillLevelsEntity entity = new AllSkillLevelsEntity();
        AllSkillLevelsEntity.PrimaryKey pk = new AllSkillLevelsEntity.PrimaryKey();
        pk.setUserId(userId);
        pk.setSkillId(skillId);
        entity.setId(pk);

        entity.setRemember(createSkillLevelEntity(remember));
        entity.setUnderstand(createSkillLevelEntity(understand));
        entity.setApply(createSkillLevelEntity(apply));
        entity.setAnalyze(createSkillLevelEntity(analyze));
        entity.setEvaluate(createSkillLevelEntity(evaluate));
        entity.setCreate(createSkillLevelEntity(create));
        entity.setSkillValue(calculateSkillValueForAllSkillLevelsEntity(entity));

        return entity;
    }

    /**
     * Helper method to create a SkillLevelEntity with a specific value
     */
    private SkillLevelEntity createSkillLevelEntity(float value) {
        SkillLevelEntity entity = new SkillLevelEntity(value);
        entity.setValue(value);
        return entity;
    }

    /**
     * Returns the skill value for a given skillLevel by combining all 6 skillLevelEntities.
     *
     * @param skillLevel skillLevel of a skill
     * @return A float value that represents the skill value of a skill
     */
    private Float calculateSkillValueForAllSkillLevelsEntity(AllSkillLevelsEntity skillLevel) {
        List<Float> values = List.of(
            skillLevel.getRemember().getValue(),
            skillLevel.getUnderstand().getValue(),
            skillLevel.getApply().getValue(),
            skillLevel.getAnalyze().getValue(),
            skillLevel.getEvaluate().getValue(),
            skillLevel.getCreate().getValue()
        );
        List<Float> nonZeroValues = values.stream().filter(v -> v > 0f).toList();
        if (nonZeroValues.isEmpty()) {
            return 0f;
        } else {
            float sum = nonZeroValues.stream().reduce(0f, Float::sum);
            return sum / nonZeroValues.size();
        }
    }

    /**
     * Helper method to verify that events were published for a specific skill
     */
    private void verifySkillLevelEvents(List<UserSkillLevelChangedEvent> events, 
            UUID userId, UUID skillId, 
            float remember, float understand, float apply, 
            float analyze, float evaluate, float create) {
        
        List<UserSkillLevelChangedEvent> skillEvents = events.stream()
                .filter(e -> e.getSkillId().equals(skillId))
                .toList();

        assertEquals(6, skillEvents.size(), "Should have 6 events for skill " + skillId);

        verifyEvent(skillEvents, userId, skillId, BloomLevel.REMEMBER, remember);
        verifyEvent(skillEvents, userId, skillId, BloomLevel.UNDERSTAND, understand);
        verifyEvent(skillEvents, userId, skillId, BloomLevel.APPLY, apply);
        verifyEvent(skillEvents, userId, skillId, BloomLevel.ANALYZE, analyze);
        verifyEvent(skillEvents, userId, skillId, BloomLevel.EVALUATE, evaluate);
        verifyEvent(skillEvents, userId, skillId, BloomLevel.CREATE, create);
    }

    /**
     * Helper method to verify a single event
     */
    private void verifyEvent(List<UserSkillLevelChangedEvent> events, 
            UUID userId, UUID skillId, BloomLevel bloomLevel, float expectedValue) {
        UserSkillLevelChangedEvent event = events.stream()
                .filter(e -> e.getBloomLevel() == bloomLevel)
                .findFirst()
                .orElse(null);

        assertNotNull(event, "Event for " + bloomLevel + " should exist");
        assertEquals(userId, event.getUserId());
        assertEquals(skillId, event.getSkillId());
        assertEquals(bloomLevel, event.getBloomLevel());
        assertEquals(expectedValue, event.getOldValue(), 0.001f);
        assertEquals(expectedValue, event.getNewValue(), 0.001f);
    }
}
