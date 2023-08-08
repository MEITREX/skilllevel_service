package de.unistuttgart.iste.gits.skilllevel_service.persistence.repository;

import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllSkillLevelsRepository extends JpaRepository<AllSkillLevelsEntity, AllSkillLevelsEntity.PrimaryKey> {


}
