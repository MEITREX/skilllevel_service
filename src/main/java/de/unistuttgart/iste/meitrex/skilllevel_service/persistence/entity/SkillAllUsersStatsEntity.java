package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
/** * Database entity which represents for every skill the average skillValue in the course. */ 

@Entity(name = "SkillAllUsersStats") 
@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAllUsersStatsEntity { 
    
    @Id private UUID skillId; 
    
    /** * Current sum of the skill values from all users in a course with this skill. */
    @Column(nullable = false) private float skillValueSum;

    /** * Current participant count of the skill level. */
    @Column(nullable = false) private int participantCount;

}