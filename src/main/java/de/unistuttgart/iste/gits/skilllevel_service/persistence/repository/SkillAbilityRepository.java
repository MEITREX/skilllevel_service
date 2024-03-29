package de.unistuttgart.iste.gits.skilllevel_service.persistence.repository;

import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.SkillAbilityEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public interface SkillAbilityRepository extends JpaRepository<SkillAbilityEntity,SkillAbilityEntity.PrimaryKey> {
}
