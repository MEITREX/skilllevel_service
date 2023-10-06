package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.gits.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.gits.generated.dto.Assessment;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.SkillLevelEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.SkillLevelLogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Calculates the skill levels of a user for a given chapter of a course.
 */
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
     * @param chapterId            The ID of the chapter
     * @param userId               The ID of the user
     * @param allSkillLevelsEntity The current skill levels of the user
     * @return The recalculated skill levels. The entity needs to be stored in the DB by the caller!
     * @throws ContentServiceConnectionException If the connection to the content service fails
     */
    public AllSkillLevelsEntity recalculateLevels(final UUID chapterId, final UUID userId,
                                                  final AllSkillLevelsEntity allSkillLevelsEntity)
            throws ContentServiceConnectionException {

        final List<Content> contents =
                contentServiceClient.queryContentsOfChapter(userId, chapterId);

        // set all skill levels to 0 and clear their logs. We're recalculating everything from scratch
        allSkillLevelsEntity.setAnalyze(new SkillLevelEntity(0));
        allSkillLevelsEntity.setRemember(new SkillLevelEntity(0));
        allSkillLevelsEntity.setUnderstand(new SkillLevelEntity(0));
        allSkillLevelsEntity.setApply(new SkillLevelEntity(0));

        return calculate(
                allSkillLevelsEntity,
                contents.stream()
                        .filter(Assessment.class::isInstance)
                        .map(x -> (Assessment) x)
                        .toList()
        );
    }

    /**
     * Adds the points earned by a user by working on the passed assessments to the passed skill levels.
     *
     * @param allSkillLevelsEntity The skill levels entity of the user to add the points to
     * @param assessments          The assessments to calculate the points from
     * @return Returns the passed AllSkillLevelsEntity with the added points.
     */
    private AllSkillLevelsEntity calculate(final AllSkillLevelsEntity allSkillLevelsEntity,
                                           final List<Assessment> assessments) {
        // find out the total amount of skill points in the current chapter
        final AllSkillPoints totalSkillPoints = getTotalSkillPointsOfAssessments(assessments);

        for (final Assessment assessment : assessments) {
            addLogItemsToSkillLevelsForAssessment(allSkillLevelsEntity, assessment, totalSkillPoints);
        }

        // go through all 4 types of skill levels and sort their log entries by date
        for (final SkillType skillType : SkillType.values()) {
            final SkillLevelEntity skillLevelToModify = getSkillLevelEntityBySkillType(allSkillLevelsEntity, skillType);

            final List<SkillLevelLogEntry> sortedLog = new ArrayList<>(skillLevelToModify.getLog().stream()
                    .sorted(Comparator.comparing(SkillLevelLogEntry::getDate))
                    .toList());

            // calculate the value differences between the log entries
            float skillLevel = 0;
            for (final SkillLevelLogEntry currentLogEntry : sortedLog) {
                skillLevel += currentLogEntry.getDifference();
                currentLogEntry.setNewValue(skillLevel);
            }

            skillLevelToModify.setLog(sortedLog);

            if (sortedLog.isEmpty()) {
                skillLevelToModify.setValue(0);
            } else {
                skillLevelToModify.setValue(sortedLog.get(sortedLog.size() - 1).getNewValue());
            }

        }

        return allSkillLevelsEntity;
    }

    /**
     * Helper method which, given an AllSkillLevelsEntity and an assessment, adds the earned skill points of the
     * assessment to the log of the corresponding skill levels. Note that the log items are just appended to the
     * log, they are not sorted by date yet, and they "newValue" property is not set yet, as it is dependent on
     * other values in the log.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to add the log items to
     * @param assessment           The assessment to calculate the log items from
     * @param totalSkillPoints     The total amount of skill points in the current chapter
     */
    private void addLogItemsToSkillLevelsForAssessment(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                       final Assessment assessment,
                                                       final AllSkillPoints totalSkillPoints) {
        final List<SkillType> skillTypes = assessment.getAssessmentMetadata().getSkillTypes();

        final List<AssessmentRepetition> repetitionResults = calculateSkillPointsOfRepetitions(assessment);

        double highestSkillPointsTillNow = 0;
        // go over all repetitions the user has made on this content
        for (final AssessmentRepetition currentRepetition : repetitionResults) {
            // only add this repetition to the skill level log if the user has improved compared to previous ones
            if (currentRepetition.earnedSkillPoints <= highestSkillPointsTillNow) {
                continue;
            }

            final List<UUID> contentIds = new ArrayList<>(1);
            contentIds.add(assessment.getId());

            // add the log entry to the skill level. For now only add the skill points earned ("difference")
            // because the order of the log will not be correct for now because we go through each assessment
            // one by one. Later we will sort the log entries by the timestamp and calculate the missing values
            for (final SkillType skillType : skillTypes) {
                // each chapter has a maximum of 10 skill levels, so we need to scale the earned skill points relative
                // to the total skill points of the chapter times the 10 levels to calculate how many levels the user
                // will gain
                final double relativeSkillPoints
                        = 10f * (currentRepetition.earnedSkillPoints() / totalSkillPoints.getValueBySkillType(skillType));
                final double relativeSkillPointsPrevious
                        = 10f * (highestSkillPointsTillNow / totalSkillPoints.getValueBySkillType(skillType));

                final SkillLevelEntity skillLevelToModify = getSkillLevelEntityBySkillType(allSkillLevelsEntity, skillType);
                skillLevelToModify.getLog().add(SkillLevelLogEntry.builder()
                        .date(currentRepetition.timestamp)
                        .difference((float) (relativeSkillPoints - relativeSkillPointsPrevious))
                        .associatedContentIds(contentIds)
                        .build());
            }

            highestSkillPointsTillNow = currentRepetition.earnedSkillPoints;
        }
    }

    /**
     * Helper method which returns the correct SkillLevelEntity of an AllSkillLevelsEntity for a given skill type.
     *
     * @param allSkillLevelsEntity The AllSkillLevelsEntity to get the SkillLevelEntity from
     * @param skillType            The skill type to get the SkillLevelEntity for
     * @return Returns the SkillLevelEntity of the passed AllSkillLevelsEntity for the passed skill type.
     */
    private SkillLevelEntity getSkillLevelEntityBySkillType(final AllSkillLevelsEntity allSkillLevelsEntity,
                                                            final SkillType skillType) {
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
     *
     * @param assessment The assessment to calculate the skill points for
     * @return Returns a list of AssessmentRepetition objects, each containing the timestamp of the repetition and
     * the earned skill points for that repetition.
     */
    private List<AssessmentRepetition> calculateSkillPointsOfRepetitions(final Assessment assessment) {
        final List<ProgressLogItem> log = assessment.getUserProgressData().getLog();
        final List<AssessmentRepetition> result = new ArrayList<>(log.size());

        // null if the assessment should not be repeated
        final Integer initialLearningInterval = assessment.getAssessmentMetadata().getInitialLearningInterval();

        // go over the log and for each repetition, check how many points the user earned for it
        // at the time of completion
        for (final ProgressLogItem currentLogItem : log) {
            double modifiers = 1.0f;

            // -10% for each hint used, but never less than 60%
            modifiers *= Math.max(1.0 - 0.1 * currentLogItem.getHintsUsed(), 0.6);

            modifiers *= currentLogItem.getCorrectness();

            // If this is a repeating assessment, the user will get fewer points for the first repetitions.
            // For this we now need to figure out how many repetitions of the content the student has done previously
            // to the current one (because repetitions give less points until the student reaches a certain number of
            // repetitions)
            if (initialLearningInterval != null) {
                final int countedRepetitions = determineCurrentRepetitionCount(assessment, currentLogItem);

                // multiply by the current repetition count compared to the needed repetition count
                // e.g. for repetition 2 of 3 the user will get 2/3 of the points
                modifiers *= (float) Math.min(countedRepetitions, REPETITIONS_NEEDED) / REPETITIONS_NEEDED;
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
     *
     * @param assessment     The assessment the currentLogItem is located in
     * @param currentLogItem The current log item up to which the number of repetitions should be determined
     * @return Returns an integer containing the number of "valid" repetitions the user has done up to the
     * current log item.
     */
    private static int determineCurrentRepetitionCount(final Assessment assessment, final ProgressLogItem currentLogItem) {
        final List<ProgressLogItem> log = assessment.getUserProgressData().getLog();

        int countedRepetitions = 0;
        OffsetDateTime lastCountedRepetitionTime = null;
        // loop over the log items up to the current one
        for (final ProgressLogItem logItem : log) {
            if (lastCountedRepetitionTime == null) {
                // for the first time the content was done, we don't need to check the time gap
                lastCountedRepetitionTime = logItem.getTimestamp();
                countedRepetitions++;
            } else {
                // learning interval is doubled for each repetition
                final Duration minimumLearningInterval = Duration.ofDays(
                        (long) (assessment.getAssessmentMetadata().getInitialLearningInterval() * Math.pow(2, countedRepetitions - 1.0))
                );

                // if the time gap between the current log item and the previous one is large enough, count it
                // as a repetition
                if (logItem.getTimestamp().isAfter(lastCountedRepetitionTime.plus(minimumLearningInterval))) {
                    lastCountedRepetitionTime = logItem.getTimestamp();
                    countedRepetitions++;
                }
            }

            // stop iterating over the log when we have reached the current log item
            if (logItem == currentLogItem) {
                break;
            }
        }

        return countedRepetitions;
    }

    /**
     * Helper method which sums up all skill points of the passed assessments.
     *
     * @param assessments The assessments to get the skill points from.
     * @return Returns an AllSkillPoints object containing float values for each skill type containing the sum of all
     * skill points of the passed assessments for that type.
     */
    private static AllSkillPoints getTotalSkillPointsOfAssessments(final List<Assessment> assessments) {
        final AllSkillPoints skillPoints = new AllSkillPoints();

        for (final Assessment assessment : assessments) {
            for (final SkillType skillType : assessment.getAssessmentMetadata().getSkillTypes()) {
                final int value = assessment.getAssessmentMetadata().getSkillPoints();
                switch (skillType) {
                    case REMEMBER -> skillPoints.remember += value;
                    case UNDERSTAND -> skillPoints.understand += value;
                    case APPLY -> skillPoints.apply += value;
                    case ANALYSE -> skillPoints.analyze += value;
                }
            }
        }
        return skillPoints;
    }

    /**
         * Helper class which stores the timestamp and the earned skill points of a repetition of an assessment during
         * the calculation.
         */
        private record AssessmentRepetition(OffsetDateTime timestamp, double earnedSkillPoints) {
    }

    /**
     * Helper class which can be used to track skill points of all types. Should not be used outside of this
     * class because it has no encapsulation. Only used inside this parent class to make calculation methods more tidy.
     */
    private static class AllSkillPoints {
        private double remember = 0;
        private double understand = 0;
        private double apply = 0;
        private double analyze = 0;

        public double getValueBySkillType(final SkillType skillType) {
            return switch (skillType) {
                case UNDERSTAND -> understand;
                case REMEMBER -> remember;
                case APPLY -> apply;
                case ANALYSE -> analyze;
            };
        }
    }
}
