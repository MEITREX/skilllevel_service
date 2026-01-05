package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
/** * Database entity which represents for every skill the average skillValue in the course. */ 

@Entity(name = "SkillAverageValue") 
@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAverageValueEntity { 
    
    @Id private UUID skillId; 
    
    /** * Current average value of the skill level. Can range from 0 to 1. */
    @Column(nullable = false) private float averageValue;

    /** * Current participant count of the skill level. */
    @Column(nullable = false) private int participantCount;

}