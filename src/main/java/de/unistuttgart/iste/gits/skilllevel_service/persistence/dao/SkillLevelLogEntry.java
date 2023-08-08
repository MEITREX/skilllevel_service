package de.unistuttgart.iste.gits.skilllevel_service.persistence.dao;

import de.unistuttgart.iste.gits.generated.dto.SkillLevelChangeReason;
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

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(nullable = false)
    private int difference;

    @Column(nullable = false)
    private int oldValue;

    @Column(nullable = false)
    private int newValue;

    @Column(nullable = false)
    private SkillLevelChangeReason reason;

    @ElementCollection
    private List<UUID> associatedContentIds;
}
