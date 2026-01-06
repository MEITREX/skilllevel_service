package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
/** * Database entity which represents for every skill the skillValue in the course. */

@Entity(name = "SkillValue") 
@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillValueEntity { 
    
    @Id private UUID skillId; 
    
    /** * Current skill value. Can range from 0 to 1. */
    @Column(nullable = false) private float skillValue;

}