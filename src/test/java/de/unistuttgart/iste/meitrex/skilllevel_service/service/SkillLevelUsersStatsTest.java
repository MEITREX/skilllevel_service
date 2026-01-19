package de.unistuttgart.iste.meitrex.skilllevel_service.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unistuttgart.iste.meitrex.skilllevel_service.service.calculation.SkillLevelCalculator;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillValueEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAllUsersStatsEntity;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.mapper.SkillLevelMapper;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.AllSkillLevelsRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.ItemDifficultyRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillAbilityRepository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository.SkillsForCourseRepository;
import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.skilllevel_service.service.SkillLevelService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SkillLevelUsersStatsTest {
    
    private final AllSkillLevelsRepository skillLevelsRepository = Mockito.mock(AllSkillLevelsRepository.class);
    private final SkillLevelMapper skillLevelMapper = Mockito.mock(SkillLevelMapper.class);
    private final SkillLevelCalculator skillLevelCalculator = Mockito.mock(SkillLevelCalculator.class);
    private final SkillAbilityRepository skillAbilityRepository = Mockito.mock(SkillAbilityRepository.class);
    private final ItemDifficultyRepository itemDifficultyRepository = Mockito.mock(ItemDifficultyRepository.class);
    private final SkillsForCourseRepository skillsForCourseRepository = Mockito.mock(SkillsForCourseRepository.class);
    private final TopicPublisher topicPublisher = Mockito.mock(TopicPublisher.class);
    
    private final SkillLevelService skillLevelService = new SkillLevelService(
            skillLevelsRepository, 
            skillLevelMapper, 
            skillLevelCalculator,
            skillAbilityRepository,
            itemDifficultyRepository,
            skillsForCourseRepository,
            topicPublisher
    );

    @Test
    void testGetSkillValuesForSkillIds() {
        final UUID userId = UUID.randomUUID();
        final UUID skillId1 = UUID.randomUUID();
        final UUID skillId2 = UUID.randomUUID();
        final UUID skillId3 = UUID.randomUUID();
        final UUID skillId4 = UUID.randomUUID();
        List<UUID> skillIds = List.of(skillId1, skillId2, skillId3, skillId4);

        AllSkillLevelsEntity skillLevel1 = createAllSkillLevelEntity(userId, skillId1, 1f, 1f, 1f, 1f, 1f, 1f);
        AllSkillLevelsEntity skillLevel2 = createAllSkillLevelEntity(userId, skillId2, 0f, 0f, 0f, 0f, 0f, 0f);
        AllSkillLevelsEntity skillLevel3 = createAllSkillLevelEntity(userId, skillId3, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f);
        AllSkillLevelsEntity skillLevel4 = createAllSkillLevelEntity(userId, skillId4, 0f, 0f, 0f, 0.5f, 0f, 0f);
        
        List<AllSkillLevelsEntity> skillLevels = List.of(skillLevel1, skillLevel2, skillLevel3, skillLevel4);

        final List<AllSkillLevelsEntity.PrimaryKey> primaryKeys = skillIds.stream()
            .map(x -> new AllSkillLevelsEntity.PrimaryKey(x, userId))
            .toList();
        when(skillLevelsRepository.findAllById(primaryKeys))
            .thenReturn(skillLevels);

        List<SkillValueEntity> skillValues = skillLevelService.getSkillValuesForSkillIds(skillIds, userId);

        assertThat(skillValues).hasSize(4);
        assertThat(skillValues).extracting(SkillValueEntity::getSkillId, SkillValueEntity::getSkillValue)
            .containsExactlyInAnyOrder(
                tuple(skillId1, 1f),
                tuple(skillId2, 0f),
                tuple(skillId3, 0.55f),
                tuple(skillId4, 0.5f)
            );
        verify(skillLevelsRepository, times(1)).findAllById(primaryKeys);
    }

    @Test
    void testGetSkillsAllUsersStatsForSkillIds() {
        final UUID user1Id = UUID.randomUUID();
        final UUID user2Id = UUID.randomUUID();
        final UUID user3Id = UUID.randomUUID();
        
        final UUID skillId1 = UUID.randomUUID();
        final UUID skillId2 = UUID.randomUUID();
        final UUID skillId3 = UUID.randomUUID();
        List<UUID> skillIds = List.of(skillId1, skillId2, skillId3);

        AllSkillLevelsEntity user1Skill1 = createAllSkillLevelEntity(user1Id, skillId1, 1f, 1f, 1f, 1f, 1f, 1f);
        AllSkillLevelsEntity user2Skill1 = createAllSkillLevelEntity(user2Id, skillId1, 1f, 1f, 1f, 1f, 1f, 1f);

        AllSkillLevelsEntity user1Skill2 = createAllSkillLevelEntity(user1Id, skillId2, 0f, 0f, 0f, 0f, 0f, 0f);
        AllSkillLevelsEntity user2Skill2 = createAllSkillLevelEntity(user2Id, skillId2, 0f, 0f, 0f, 0f, 0f, 0f);

        AllSkillLevelsEntity user1Skill3 = createAllSkillLevelEntity(user1Id, skillId3, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f);
        AllSkillLevelsEntity user2Skill3 = createAllSkillLevelEntity(user2Id, skillId3, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f);
        AllSkillLevelsEntity user3Skill3 = createAllSkillLevelEntity(user3Id, skillId3, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f);

        when(skillLevelsRepository.findByIdSkillId(skillId1))
            .thenReturn(List.of(user1Skill1, user2Skill1));
        when(skillLevelsRepository.findByIdSkillId(skillId2))
            .thenReturn(List.of(user1Skill2, user2Skill2));
        when(skillLevelsRepository.findByIdSkillId(skillId3))
            .thenReturn(List.of(user1Skill3, user2Skill3, user3Skill3));

        List<SkillAllUsersStatsEntity> skillAllUsersStats = skillLevelService.getSkillsAllUsersStatsForSkillIds(skillIds);

        assertThat(skillAllUsersStats).hasSize(3);
        assertThat(skillAllUsersStats).extracting(SkillAllUsersStatsEntity::getSkillId, SkillAllUsersStatsEntity::getSkillValueSum, SkillAllUsersStatsEntity::getParticipantCount)
            .containsExactlyInAnyOrder(
                tuple(skillId1, 2f, 2),
                tuple(skillId2, 0f, 2),
                tuple(skillId3, 1.75f, 3)
            );
        
        verify(skillLevelsRepository, times(1)).findByIdSkillId(skillId1);
        verify(skillLevelsRepository, times(1)).findByIdSkillId(skillId2);
        verify(skillLevelsRepository, times(1)).findByIdSkillId(skillId3);
    }

    /**
     * Helper method to create a skill level entity with specified values
     */
    private AllSkillLevelsEntity createAllSkillLevelEntity(UUID userId, UUID skillId, 
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
}
