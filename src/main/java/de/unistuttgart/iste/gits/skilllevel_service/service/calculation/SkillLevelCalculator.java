package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Assessment;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillLevelCalculator {

    private static final double MAX_CORRECTNESS_MODIFIER = 1.1;
    private static final double MIN_CORRECTNESS_MODIFIER = 0.6;
    private static final double MAX_TIME_MODIFIER = 1.5;
    private static final double MIN_TIME_MODIFIER = 0.5;

    public AllSkillLevelsEntity recalculateLevels(AllSkillLevelsEntity allSkillLevelsEntity, List<Content> contents, int chapterCount) {
        return calculate(allSkillLevelsEntity, contents, chapterCount);
    }

    public AllSkillLevelsEntity calculateOnContentWorkedOn(AllSkillLevelsEntity allSkillLevelsEntity, List<Content> contents, UserProgressLogEvent event, int chapterCount) {
        return calculate(allSkillLevelsEntity, contents, chapterCount);
    }

    private AllSkillLevelsEntity calculate(AllSkillLevelsEntity allSkillLevelsEntity, List<Content> contents, int chapterCount) {
        SkillLevelEntity rememberLevel = allSkillLevelsEntity.getRemember();
        SkillLevelEntity understandLevel = allSkillLevelsEntity.getUnderstand();
        SkillLevelEntity applyLevel = allSkillLevelsEntity.getApply();
        SkillLevelEntity analyzeLevel = allSkillLevelsEntity.getAnalyze();

        // find out the total amount of skill points in the current chapter
        double totalSkillPoints = 0;
        for(Content content : contents) {
            if(!(content instanceof Assessment assessment)) continue; // Skip all contents that are not assessments

            totalSkillPoints += assessment.getAssessmentMetadata().getSkillPoints();
        }

        for (Content content : contents) {
            if(!(content instanceof Assessment assessment)) continue; // Skip all contents that are not assessments

            ProgressLogItem logItem = content.getUserProgressData().getLog().get(content.getUserProgressData().getLog().size() - 1);
            double skillPoints = assessment.getAssessmentMetadata().getSkillPoints();
            double modifiedSkillPoints = applyModifiers(logItem, skillPoints);

            double relativeSkillPoints = (100.0 / chapterCount) * (modifiedSkillPoints / totalSkillPoints);

            SkillType skillType = assessment.getAssessmentMetadata().getSkillType();

            switch(skillType) {
                case UNDERSTAND -> understandLevel.setValue(understandLevel.getValue() + (float) relativeSkillPoints);
                case REMEMBER -> rememberLevel.setValue(rememberLevel.getValue() + (float) relativeSkillPoints);
                case APPLY -> applyLevel.setValue(applyLevel.getValue() + (float) relativeSkillPoints);
                case ANALYSE -> analyzeLevel.setValue(analyzeLevel.getValue() + (float) relativeSkillPoints);
            }
        }

        return allSkillLevelsEntity;
    }

    private double applyModifiers(ProgressLogItem logItem, double skillPoints) {
        double modifiers = 1.0;

        // Apply modifiers for hints used
        modifiers *= Math.max(1.0 - 0.1 * logItem.getHintsUsed(), 0.6);

        // Apply modifier for correctness
        double correctnessModifier = Math.min(MAX_CORRECTNESS_MODIFIER,
                Math.max(MIN_CORRECTNESS_MODIFIER, 0.1 * logItem.getCorrectness() + 0.6));
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
