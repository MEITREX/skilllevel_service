package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;

import java.util.List;

public interface LevelCalculator {


    SkillLevelEntity recalculateLevel(SkillLevelEntity skillLevel, List<Content> contents);


}
