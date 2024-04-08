package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;
/**
 * Database entity which represents the difficulty of an item, as well as the number of attempts on this item.
 */
@Entity(name = "ItemDifficulty")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemDifficultyEntity {

    @Id
    private UUID itemId;
    @Column(nullable = false)
    private float difficulty;

    @Column(nullable = false)
    private int numberOfPreviousAttempts;

}
