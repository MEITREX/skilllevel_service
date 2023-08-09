package de.unistuttgart.iste.gits.skilllevel_service.service.calculation;

/**
 * Exception thrown when an error occurs during the calculation of a reward score.
 */
public class SkillLevelCalculationException extends RuntimeException {

    public SkillLevelCalculationException(String message) {
        super(message);
    }

    public SkillLevelCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
