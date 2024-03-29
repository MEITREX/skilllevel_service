package de.unistuttgart.iste.gits.skilllevel_service.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Database entity which represents a log entry for a particular skill level of a user for a chapter. Stores at
 * what time the skill level was changed, by how much it was changed and which content was associated with the
 * change.
 */
@Entity(name = "SkillLevelLogEntry")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillLevelLogEntry {
    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Date and time when the skill level was changed.
     */
    @Column(nullable = false)
    private OffsetDateTime date;

    /**
     * Difference between the old and the new value of the skill level.
     */
    @Column(nullable = false)
    private double difference;

    /**
     * @return The old value of the skill level before the change.
     */
    public double getOldValue() {
        return newValue - difference;
    }

    /**
     * New value of the skill level after the change.
     */
    @Column(nullable = false)
    private double newValue;

    /**
     * Id of the items which caused this change in the skill level.
     */
    @Column( nullable = false)
    private UUID associatedItemId;
}
