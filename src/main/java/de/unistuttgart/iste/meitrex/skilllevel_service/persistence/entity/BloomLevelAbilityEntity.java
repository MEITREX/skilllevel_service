package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;

import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

/**
 * Database entity which represents the ability of a user of a level of Blooms Taxonomy.
 */

@Entity(name = "BloomLevelAbility")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloomLevelAbilityEntity {

    public BloomLevelAbilityEntity(UUID userId, BloomLevel bloomLevel, float ability, int numberOfPreviousAttempts) {
        this.userId = userId;
        this.bloomLevel = bloomLevel;
        this.ability = ability;
        this.numberOfPreviousAttempts = numberOfPreviousAttempts;
        this.id = UUID.randomUUID();
    }

    @Generated
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BloomLevel bloomLevel;

    @Column(nullable = false)
    private float ability;

    @Column(nullable = false)
    private int numberOfPreviousAttempts;


}
