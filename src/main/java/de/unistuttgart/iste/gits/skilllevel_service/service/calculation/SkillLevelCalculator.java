package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.generated.dto.Assessment;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.SkillLevelLogEntry;
import de.unistuttgart.iste.gits.skilllevel_service.service.ContentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SkillLevelCalculator {

    private final ContentServiceClient contentServiceClient;

    // 3 repetitions needed for full points.
    // TODO: This is hard-coded for now, but could be made configurable per assessment in the future
    static final int REPETITIONS_NEEDED = 3;

    /**
     * Recalculates the skill levels for a given user and chapter of a course.
     *
     * @param chapterId The ID of the chapter
     * @param userId The ID of the user
     * @param allSkillLevelsEntity The current skill levels of the user
     * @return The recalculated skill levels. The entity needs to be stored in the DB by the caller!
     */
    public AllSkillLevelsEntity recalculateLevels(UUID chapterId, UUID userId,
                                                  AllSkillLevelsEntity allSkillLevelsEntity) {
        List<Content> contents =
                contentServiceClient.getContentsOfChapter(userId, chapterId);

        // set all skill levels to 0 and clear their logs. We're recalculating everything from scratch
        allSkillLevelsEntity.setAnalyze(new SkillLevelEntity(0));
        allSkillLevelsEntity.setRemember(new SkillLevelEntity(0));
        allSkillLevelsEntity.setUnderstand(new SkillLevelEntity(0));
        allSkillLevelsEntity.setApply(new SkillLevelEntity(0));

        return calculate(
                allSkillLevelsEntity,
                contents.stream()
                        .filter(Assessment.class::isInstance)
                        .map(x -> (Assessment)x)
                        .toList()
        );
    }

    /**
     * Adds the points earned by a user by working on the passed assessments to the passed skill levels.
     * @param allSkillLevelsEntity The skill levels entity of the user to add the points to
     * @param assessments The assessments to calculate the points from
     * @return Returns the passed AllSkillLevelsEntity with the added points.
     */
    private AllSkillLevelsEntity calculate(AllSkillLevelsEntity allSkillLevelsEntity,
                                           List<Assessment> assessments) {
        // find out the total amount of skill points in the current chapter
        float totalSkillPoints = getTotalSkillPointsOfAssessments(assessments);

        if(totalSkillPoints == 0) {
            return allSkillLevelsEntity;
        }

        for (Assessment assessment : assessments) {
            addLogItemsToSkillLevelsForAssessment(allSkillLevelsEntity, assessment, totalSkillPoints);
        }

        // go through all 4 types of skill levels and sort their log entries by date
        for(SkillType skillType : SkillType.values())  {
            SkillLevelEntity skillLevelToModify = getSkillLevelEntityBySkillType(allSkillLevelsEntity, skillType);

            List<SkillLevelLogEntry> sortedLog = new ArrayList<>(skillLevelToModify.getLog().stream()
                    .sorted(Comparator.comparing(SkillLevelLogEntry::getDate))
                    .toList());

            // calculate the value differences between the log entries
            float skillLevel = 0;
            for(SkillLevelLogEntry currentLogEntry : sortedLog) {
                skillLevel += currentLogEntry.getDifference();
                currentLogEntry.setNewValue(skillLevel);
            }

            skillLevelToModify.setLog(sortedLog);

            if(!sortedLog.isEmpty()) {
                skillLevelToModify.setValue(sortedLog.get(sortedLog.size() - 1).getNewValue());
            } else {
                skillLevelToModify.setValue(0);
            }

        }

        return allSkillLevelsEntity;
    }

    /**
     * Helper method which, given an AllSkillLevelsEntity and an assessment, adds the earned skill points of the
     * assessment to the log of the corresponding skill levels. Note that the log items are just appended to the
     * log, they are not sorted by date yet, and they "newValue" property is not set yet, as it is dependent on
     * other values in the log.
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to add the log items to
     * @param assessment The assessment to calculate the log items from
     * @param totalSkillPoints The total amount of skill points in the current chapter
     */
    private void addLogItemsToSkillLevelsForAssessment(AllSkillLevelsEntity allSkillLevelsEntity,
                                                       Assessment assessment,
                                                       float totalSkillPoints) {
        List<SkillType> skillTypes = assessment.getAssessmentMetadata().getSkillTypes();
        List<SkillLevelEntity> skillLevelsToModify =
                skillTypes.stream().map(x -> getSkillLevelEntityBySkillType(allSkillLevelsEntity, x)).toList();

        List<AssessmentRepetition> repetitionResults = calculateSkillPointsOfRepetitions(assessment);

        float highestSkillPointsTillNow = 0;
        // go over all repetitions the user has made on this content
        for(AssessmentRepetition currentRepetition : repetitionResults) {
            // each chapter has a maximum of 10 skill levels, so we need to scale the earned skill points relative
            // to the total skill points of the chapter times the 10 levels to calculate how many levels the user
            // will gain
            float relativeSkillPoints = 10.f * (currentRepetition.earnedSkillPoints / totalSkillPoints);

            // only add this repetition to the skill level log if the user has improved compared to previous ones
            if(relativeSkillPoints <= highestSkillPointsTillNow)
                continue;

            List<UUID> contentIds = new ArrayList<>(1);
            contentIds.add(assessment.getId());

            // add the log entry to the skill level. For now only add the skill points earned ("difference")
            // because the order of the log will not be correct for now because we go through each assessment
            // one by one. Later we will sort the log entries by the timestamp and calculate the missing values
            for(SkillLevelEntity skillLevelToModify : skillLevelsToModify) {
                skillLevelToModify.getLog().add(SkillLevelLogEntry.builder()
                        .date(currentRepetition.timestamp)
                        .difference(relativeSkillPoints - highestSkillPointsTillNow)
                        .associatedContentIds(contentIds)
                        .build());
            }

            highestSkillPointsTillNow = relativeSkillPoints;
        }
    }

    /**
     * Helper method which returns the correct SkillLevelEntity of an AllSkillLevelsEntity for a given skill type.
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to get the SkillLevelEntity from
     * @param skillType The skill type to get the SkillLevelEntity for
     * @return Returns the SkillLevelEntity of the passed AllSkillLevelsEntity for the passed skill type.
     */
    private SkillLevelEntity getSkillLevelEntityBySkillType(AllSkillLevelsEntity allSkillLevelsEntity,
                                                            SkillType skillType) {
        return switch (skillType) {
            case UNDERSTAND -> allSkillLevelsEntity.getUnderstand();
            case REMEMBER -> allSkillLevelsEntity.getRemember();
            case APPLY -> allSkillLevelsEntity.getApply();
            case ANALYSE -> allSkillLevelsEntity.getAnalyze();
        };
    }

    /**
     * Helper method which calculates the history of skill points earned by a user through repetition of a
     * given assessment.
     * @param assessment The assessment to calculate the skill points for
     * @return Returns a list of AssessmentRepetition objects, each containing the timestamp of the repetition and
     *         the earned skill points for that repetition.
     */
    private List<AssessmentRepetition> calculateSkillPointsOfRepetitions(Assessment assessment) {
        List<ProgressLogItem> log = assessment.getUserProgressData().getLog();
        List<AssessmentRepetition> result = new ArrayList<>(log.size());

        // null if the assessment should not be repeated
        Integer initialLearningInterval = assessment.getAssessmentMetadata().getInitialLearningInterval();

        // go over the log and for each repetition, check how many points the user earned for it
        // at the time of completion
        for(int i = 0; i < log.size(); i++) {
            ProgressLogItem currentLogItem = log.get(i);

            float modifiers = 1.0f;

            // -10% for each hint used, but never less than 60%
            modifiers *= Math.max(1.0 - 0.1 * currentLogItem.getHintsUsed(), 0.6);

            modifiers *= currentLogItem.getCorrectness();

            // If this is a repeating assessment, the user will get fewer points for the first repetitions.
            // For this we now need to figure out how many repetitions of the content the student has done previously
            // to the current one (because repetitions give less points until the student reaches a certain number of
            // repetitions)
            if(initialLearningInterval != null) {
                int countedRepetitions = determineCurrentRepetitionCount(assessment, currentLogItem);

                // multiply by the current repetition count compared to the needed repetition count
                // e.g. for repetition 2 of 3 the user will get 2/3 of the points
                modifiers *= (float)Math.min(countedRepetitions, REPETITIONS_NEEDED) / REPETITIONS_NEEDED;
            }

            // TODO: In the future, time taken to complete should also be taken into account. For this, time tracking
            // in the frontend and a time limit property in the assessment metadata will be necessary

            result.add(new AssessmentRepetition(
                    currentLogItem.getTimestamp(),
                    modifiers * assessment.getAssessmentMetadata().getSkillPoints()));
        }

        return result;
    }

    /**
     * Helper method which determines for a given log item of an assessment how many repetitions the user has done
     * up to that point (including the current one). Note that repetitions are only counted if they are far enough
     * apart in time. The necessary time interval is dependent on the initialLearningInterval property of the
     * assessment, the second repetition will need to be at least initialLearningInterval days after the first, and
     * for each further repetition the necessary learning interval will continue to double.
     * @param assessment The assessment the currentLogItem is located in
     * @param currentLogItem The current log item up to which the number of repetitions should be determined
     * @return Returns an integer containing the number of "valid" repetitions the user has done up to the
     *         current log item.
     */
    private static int determineCurrentRepetitionCount(Assessment assessment, ProgressLogItem currentLogItem) {
        List<ProgressLogItem> log = assessment.getUserProgressData().getLog();

        int countedRepetitions = 0;
        OffsetDateTime lastCountedRepetitionTime = null;
        // loop over the log items up to the current one
        for (ProgressLogItem logItem : log) {
            if(lastCountedRepetitionTime == null) {
                // for the first time the content was done, we don't need to check the time gap
                lastCountedRepetitionTime = logItem.getTimestamp();
                countedRepetitions++;
            } else {
                // learning interval is doubled for each repetition
                Duration minimumLearningInterval = Duration.ofDays(
                        (long)(assessment.getAssessmentMetadata().getInitialLearningInterval() * Math.pow(2, countedRepetitions - 1.0))
                );

                // if the time gap between the current log item and the previous one is large enough, count it
                // as a repetition
                if(logItem.getTimestamp().isAfter(lastCountedRepetitionTime.plus(minimumLearningInterval))) {
                    lastCountedRepetitionTime = logItem.getTimestamp();
                    countedRepetitions++;
                }
            }

            // stop iterating over the log when we have reached the current log item
            if(logItem == currentLogItem) {
                break;
            }
        }

        return countedRepetitions;
    }

    /**
     * Helper method which sums up all skill points of the passed assessments.
     * @param assessments The assessments to get the skill points from.
     * @return Returns an integer containing the sum of all skill points of the passed assessments.
     */
    private static int getTotalSkillPointsOfAssessments(List<Assessment> assessments) {
        int totalSkillPoints = 0;
        for(Assessment assessment : assessments) {
            totalSkillPoints += assessment.getAssessmentMetadata().getSkillPoints();
        }
        return totalSkillPoints;
    }

    /**
     * Helper class which stores the timestamp and the earned skill points of a repetition of an assessment during
     * the calculation.
     */
    private static class AssessmentRepetition {
        public final OffsetDateTime timestamp;
        public final float earnedSkillPoints;

        public AssessmentRepetition(OffsetDateTime timestamp, float earnedSkillPoints) {
            this.timestamp = timestamp;
            this.earnedSkillPoints = earnedSkillPoints;
        }
    }
}
