package de.unistuttgart.iste.gits.skilllevel_service.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "SkillLevelLogEntry")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillLevelLogEntry {

    public float getOldValue() {
        return newValue - difference;
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(nullable = false)
    private float difference;

    @Column(nullable = false)
    private float newValue;

    @Column(columnDefinition = "UUID[]", nullable = false)
    private List<UUID> associatedContentIds;
}
