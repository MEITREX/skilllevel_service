package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Database entity which represents the ability of a user of a skill.
 */

@Entity(name = "SkillAbility")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAbilityEntity {
    @EmbeddedId
    private PrimaryKey id;
    @Column(nullable = false)
    private float ability;

    @Column(nullable = false)
    private int numberOfPreviousAttempts;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrimaryKey implements Serializable {
        private UUID skillId;
        private UUID userId;
    }

}
