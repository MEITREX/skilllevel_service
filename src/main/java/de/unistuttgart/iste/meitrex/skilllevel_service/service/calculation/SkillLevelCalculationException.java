package de.unistuttgart.iste.meitrex.skilllevel_service.service.calculation;

/**
 * Exception thrown when an error occurs during the calculation of a skill level.
 */
public class SkillLevelCalculationException extends RuntimeException {

    public SkillLevelCalculationException(String message) {
        super(message);
    }

    public SkillLevelCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
