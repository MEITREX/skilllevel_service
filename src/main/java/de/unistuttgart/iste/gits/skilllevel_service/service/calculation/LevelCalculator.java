package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;

import java.util.List;

public interface LevelCalculator {

    /**
     * Recalculation that is done every night.
     *
     * @param allSkillLevelsEntity all reward scores
     * @param contents        all contents of the course
     * @return the new reward score
     */
    SkillLevelEntity recalculateLevel(AllSkillLevelsEntity allSkillLevelsEntity, List<Content> contents);

    /**
     * Calculation that is done when a user works on a content.
     *
     * @param allSkillLevelsEntity all reward scores
     * @param contents        all contents of the course
     * @param event           the event that triggered the calculation
     * @return the new reward score
     */
    SkillLevelEntity calculateOnContentWorkedOn(AllSkillLevelsEntity allSkillLevelsEntity,
                                                 List<Content> contents,
                                                 UserProgressLogEvent event);


}

