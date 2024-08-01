package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

/**
 * Database entity that saves for a course the corresponding skills
 */
@Entity(name = "SkillsForCourse")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillsForCourse {

    @Id
    @Generated
    private UUID id;

    @Column(nullable = false)
    private UUID skillId;

    @Column
    private UUID courseId;
}
