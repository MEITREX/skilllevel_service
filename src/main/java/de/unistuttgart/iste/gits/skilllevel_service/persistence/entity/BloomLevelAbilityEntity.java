package de.unistuttgart.iste.gits.skilllevel_service.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

/**
 * Database entity which represents the ability of a user of a level of Blooms Taxonomy.
 */

@Entity(name="BloomLevelAbility")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloomLevelAbilityEntity {

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
