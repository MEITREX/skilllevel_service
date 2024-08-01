package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository;

import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.SkillsForCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillsForCourseRepository extends JpaRepository<SkillsForCourse, UUID> {
    List<SkillsForCourse> findByCourseId(UUID courseId);
}
