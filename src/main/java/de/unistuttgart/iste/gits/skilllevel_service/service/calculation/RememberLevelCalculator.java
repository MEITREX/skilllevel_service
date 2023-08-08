package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RememberLevelCalculator implements LevelCalculator {
    @Override
    public SkillLevelEntity recalculateLevel(SkillLevelEntity skillLevel, List<Content> contents) {
        return skillLevel;
    }


}