package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillLevelCalculator implements LevelCalculator {

    private static final double MAX_CORRECTNESS_MODIFIER = 1.1;
    private static final double MIN_CORRECTNESS_MODIFIER = 0.6;
    private static final double MAX_TIME_MODIFIER = 1.5;
    private static final double MIN_TIME_MODIFIER = 0.5;

    @Override
    public SkillLevelEntity recalculateLevel(AllSkillLevelsEntity allSkillLevelsEntity, List<Content> contents) {
        SkillLevelEntity rememberLevel = allSkillLevelsEntity.getRemember();
        SkillLevelEntity understandLevel = allSkillLevelsEntity.getUnderstand();
        SkillLevelEntity applyLevel = allSkillLevelsEntity.getApply();
        SkillLevelEntity analyzeLevel = allSkillLevelsEntity.getAnalyze();

        for (Content content : contents) {
            UserProgressLogEvent event = content.getUserProgressLogEvent();
            double skillPoints = calculateSkillPoints(content);
            double modifiedSkillPoints = applyModifiers(event, skillPoints);

            rememberLevel.setValue(rememberLevel.getValue() + (float) modifiedSkillPoints);
            understandLevel.setValue(understandLevel.getValue() + (float) modifiedSkillPoints);
            applyLevel.setValue(applyLevel.getValue() + (float) modifiedSkillPoints);
            analyzeLevel.setValue(analyzeLevel.getValue() + (float) modifiedSkillPoints);
        }

        return rememberLevel; // Return any of the skill level entities
    }

    @Override
    public SkillLevelEntity calculateOnContentWorkedOn(AllSkillLevelsEntity allSkillLevelsEntity, List<Content> contents, UserProgressLogEvent event) {
        SkillLevelEntity newSkillLevel = new SkillLevelEntity();

        for (Content content : contents) {
            if (content.getId().equals(event.getContentId())) {
                double skillPoints = calculateSkillPoints(content);
                double modifiedSkillPoints = applyModifiers(event, skillPoints);

                newSkillLevel.setValue((float) (allSkillLevelsEntity.getRemember().getValue() + modifiedSkillPoints));
                break; // No need to continue looping
            }
        }

        return newSkillLevel;
    }

    private double calculateSkillPoints(Content content) {
        return content.getSkillPoints();
    }

    private double applyModifiers(UserProgressLogEvent event, double skillPoints) {
        double modifiers = 1.0;

        // Apply modifiers for hints used
        modifiers *= Math.max(1.0 - 0.1 * event.getHintsUsed(), 0.6);

        // Apply modifier for correctness
        double correctnessModifier = Math.min(MAX_CORRECTNESS_MODIFIER,
                Math.max(MIN_CORRECTNESS_MODIFIER, 0.1 * event.getCorrectness() + 0.6));
        modifiers *= correctnessModifier;

        // Apply modifier for time
         /*
    The time modifier is calculated as the ratio of the time limit of the assessment to the actual time taken by the student.
    It's limited within a certain range to prevent extreme adjustments.
    If the student completes the assessment faster than the time limit, the time modifier will be greater than 1.0, effectively increasing the skill points they earn.
    If the student takes longer than the time limit, the time modifier will be less than 1.0, decreasing the skill points they earn.
    The calculated time modifier is then multiplied with the skill points to adjust the final skill points earned for that assessment.*/

        /*double timeModifier = Math.min(MAX_TIME_MODIFIER,
                Math.max(MIN_TIME_MODIFIER, event.getTimeToComplete() / (double) event.getTimeTaken()));
        modifiers *= timeModifier;*/

        return skillPoints * modifiers;
    }
}
