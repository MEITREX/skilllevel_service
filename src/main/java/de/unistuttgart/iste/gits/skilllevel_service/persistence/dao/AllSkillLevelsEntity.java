package de.unistuttgart.iste.gits.skilllevel_service.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.UUID;

@Entity(name = "SkillLevels")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllSkillLevelsEntity {

    @EmbeddedId
    private PrimaryKey id;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private SkillLevelEntity remember;
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private SkillLevelEntity understand;
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private SkillLevelEntity apply;
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private SkillLevelEntity analyze;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrimaryKey implements Serializable {
        private UUID chapterId;
        private UUID userId;
    }
}
