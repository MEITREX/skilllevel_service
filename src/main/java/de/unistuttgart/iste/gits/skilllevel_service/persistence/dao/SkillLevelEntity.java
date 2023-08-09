package de.unistuttgart.iste.gits.skilllevel_service.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "SkillLevel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillLevelEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private float value;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("date DESC")
    @Builder.Default
    private List<SkillLevelLogEntry> log = new ArrayList<>();


}
