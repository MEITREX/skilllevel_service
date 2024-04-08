package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Database entity which represents a particular skill level (Remember, Understand, etc.) of a user for a chapter.
 */
@Entity(name = "SkillLevel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillLevelEntity {

    public SkillLevelEntity(float value) {
        this.value = value;
        this.log = new ArrayList<>();
    }

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Current value of the skill level. Can range from 0 to 1.
     */
    @Column(nullable = false)
    private float value;

    /**
     * List of log entries for this skill level. Contains information about how the value of the skill level has
     * progressed over time.
     */
    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("date DESC")
    @Builder.Default
    private List<SkillLevelLogEntry> log = new ArrayList<>();
}