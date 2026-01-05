package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillAverageValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository 
public interface SkillAverageValueRepository extends JpaRepository<SkillAverageValueEntity, UUID> {
}