package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.AllSkillLevelsEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelEntity;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.dao.SkillLevelLogEntry;
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
        List<ContentServiceClient.GenericAssessmentResponse> assessments =
                contentServiceClient.getAssessmentsOfChapter(userId, chapterId);

        // set all skill levels to 0 and clear their logs. We're recalculating everything from scratch
        allSkillLevelsEntity.setAnalyze(new SkillLevelEntity(0));
        allSkillLevelsEntity.setRemember(new SkillLevelEntity(0));
        allSkillLevelsEntity.setUnderstand(new SkillLevelEntity(0));
        allSkillLevelsEntity.setApply(new SkillLevelEntity(0));

        return calculate(allSkillLevelsEntity, assessments);
    }

    private AllSkillLevelsEntity calculate(AllSkillLevelsEntity allSkillLevelsEntity,
                                           List<ContentServiceClient.GenericAssessmentResponse> assessments) {
        if(assessments.isEmpty()) {
            return allSkillLevelsEntity;
        }

        // find out the total amount of skill points in the current chapter
        float totalSkillPoints = 0;
        for(ContentServiceClient.GenericAssessmentResponse assessment : assessments) {
            totalSkillPoints += assessment.getAssessmentMetadata().getSkillPoints();
        }

        for (ContentServiceClient.GenericAssessmentResponse assessment : assessments) {
            List<ProgressLogItem> log = assessment.getProgressDataForUser().getLog();

            SkillType skillType = assessment.getAssessmentMetadata().getSkillType();
            SkillLevelEntity skillLevelToModify = getSkillLevelEntityBySkillType(allSkillLevelsEntity, skillType);

            // if nothing in the log (i.e. the user has not worked on this content), skip it
            if(log.isEmpty()) continue;

            List<AssessmentRepetition> repetitionResults = calculateSkillPointsOfRepetitions(assessment);

            float highestSkillPointsTillNow = 0;
            // go over all repetitions the user has made on this content
            for(AssessmentRepetition currentRepetition : repetitionResults) {
                // each chapter has a maximum of 10 skill levels, so we need to scale the earned skill points relative
                // to the total skill points of the chapter times the 10 levels to calculate how many levels the user
                // will gain
                float relativeSkillPoints = 10.f * (currentRepetition.earnedSkillPoints / totalSkillPoints);

                // only add this repetition to the skill level log if the user has improved compared to previous ones
                if(relativeSkillPoints > highestSkillPointsTillNow) {
                    List<UUID> contentIds = new ArrayList<>(1);
                    contentIds.add(assessment.getId());

                    skillLevelToModify.getLog().add(SkillLevelLogEntry.builder()
                            .date(currentRepetition.timestamp)
                            .difference(relativeSkillPoints - highestSkillPointsTillNow)
                            .associatedContentIds(contentIds)
                            .build());

                    highestSkillPointsTillNow = relativeSkillPoints;
                }
            }
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

    private SkillLevelEntity getSkillLevelEntityBySkillType(AllSkillLevelsEntity allSkillLevelsEntity,
                                                            SkillType skillType) {
        return switch (skillType) {
            case UNDERSTAND -> allSkillLevelsEntity.getUnderstand();
            case REMEMBER -> allSkillLevelsEntity.getRemember();
            case APPLY -> allSkillLevelsEntity.getApply();
            case ANALYSE -> allSkillLevelsEntity.getAnalyze();
        };
    }

    private List<AssessmentRepetition> calculateSkillPointsOfRepetitions(
            ContentServiceClient.GenericAssessmentResponse assessment) {
        List<ProgressLogItem> log = assessment.getProgressDataForUser().getLog();
        List<AssessmentRepetition> result = new ArrayList<>(log.size());
        // go over the log and for each repetition, check how many points the user earned for it
        // at the time of completion
        for(int i = 0; i < log.size(); i++) {
            ProgressLogItem currentLogItem = log.get(i);

            float modifiers = 1.0f;

            // -10% for each hint used, but never less than 60%
            modifiers *= Math.max(1.0 - 0.1 * currentLogItem.getHintsUsed(), 0.6);

            modifiers *= currentLogItem.getCorrectness();

            // we now need to figure out how many repetitions of the content the student has done previously to the
            // current one. However, a repetition should only be counted if there is a large enough time gap between
            // the repetitions.
            int countedRepetitions = 0;
            OffsetDateTime lastCountedRepetitionTime = null;
            // loop over the previous log items
            for (int j = 0; j <= i; j++) {
                ProgressLogItem otherLogItem = log.get(j);
                if(lastCountedRepetitionTime == null) {
                    // for the first time the content was done, we don't need to check the time gap
                    lastCountedRepetitionTime = otherLogItem.getTimestamp();
                    countedRepetitions++;
                } else {
                    // learning interval is doubled for each repetition
                    Duration minimumLearningInterval = Duration.ofDays(
                            (long)(assessment.getAssessmentMetadata().getInitialLearningInterval() * Math.pow(2, (int)(countedRepetitions - 1)))
                    );
                    // if the time gap between the current log item and the previous one is large enough, count it
                    // as a repetition
                    if(otherLogItem.getTimestamp().isAfter(lastCountedRepetitionTime.plus(minimumLearningInterval))) {
                        lastCountedRepetitionTime = otherLogItem.getTimestamp();
                        countedRepetitions++;
                    }
                }
            }

            // 3 repetitions needed for full points.
            // TODO: This is hard-coded for now, but should be configurable per assessment in the future
            final int repetitionsNeeded = 3;
            modifiers *= (float)Math.min(countedRepetitions, repetitionsNeeded) / repetitionsNeeded;

            // TODO: Add time modifier. For this there is still an implementation of time limits missing in the
            // content service

            result.add(new AssessmentRepetition(
                    currentLogItem.getTimestamp(),
                    modifiers * assessment.getAssessmentMetadata().getSkillPoints()));
        }

        return result;
    }

    private static class AssessmentRepetition {
        public final OffsetDateTime timestamp;
        public final float earnedSkillPoints;

        public AssessmentRepetition(OffsetDateTime timestamp, float earnedSkillPoints) {
            this.timestamp = timestamp;
            this.earnedSkillPoints = earnedSkillPoints;
        }
    }
}
