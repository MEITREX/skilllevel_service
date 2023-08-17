package de.unistuttgart.iste.gits.skilllevel_service.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(nullable = false)
    private float value;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("date DESC")
    @Builder.Default
    private List<SkillLevelLogEntry> log = new ArrayList<>();
}
