package de.unistuttgart.iste.gits.skilllevel_service.persistence.repository;

import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@Profile("!test")
public interface AllSkillLevelsRepository extends JpaRepository<AllSkillLevelsEntity, AllSkillLevelsEntity.PrimaryKey> {
    List<AllSkillLevelsEntity> findByIdChapterId(UUID chapterId);
}
