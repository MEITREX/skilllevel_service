package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository 
public interface SkillValueRepository extends JpaRepository<SkillValueEntity, UUID> {
}
