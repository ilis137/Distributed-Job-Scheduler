package com.scheduler.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;

/**
 * Validator implementation for {@link ValidCronExpression} annotation.
 * 
 * Validates cron expressions using Spring's {@link CronExpression} parser
 * to ensure consistency with the scheduling logic used in JobController
 * and JobExecutor.
 * 
 * Interview Talking Points:
 * - "I implement ConstraintValidator to integrate with Jakarta Bean Validation"
 * - "The validator uses Spring's CronExpression.parse() for accurate validation"
 * - "Null and blank values are allowed since cron expressions are optional for one-time jobs"
 * - "I log validation failures at DEBUG level for troubleshooting without cluttering logs"
 * 
 * Design Decision:
 * "I chose to allow null/blank values because:
 * 1. Cron expressions are optional - jobs without them are one-time jobs
 * 2. This follows the 'fail fast' principle - only validate when a value is provided
 * 3. Required field validation is handled by @NotBlank if needed
 * 4. This makes the validator reusable for both required and optional fields"
 * 
 * Validation Logic:
 * 1. Null or blank → VALID (optional field)
 * 2. Valid cron expression → VALID
 * 3. Invalid cron expression → INVALID (logs error at DEBUG level)
 * 
 * Performance Consideration:
 * "Cron parsing is relatively fast (microseconds), so validating at the API
 * boundary doesn't add significant overhead. The benefit of catching invalid
 * expressions early outweighs the minimal performance cost."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 * @see ValidCronExpression
 * @see CronExpression
 */
public class CronExpressionValidator implements ConstraintValidator<ValidCronExpression, String> {
    
    private static final Logger log = LoggerFactory.getLogger(CronExpressionValidator.class);
    
    /**
     * Initializes the validator.
     * 
     * This method is called once when the validator is instantiated.
     * Can be used to read annotation parameters if needed.
     * 
     * @param constraintAnnotation the annotation instance
     */
    @Override
    public void initialize(ValidCronExpression constraintAnnotation) {
        // No initialization needed - we use Spring's CronExpression parser directly
        log.debug("CronExpressionValidator initialized");
    }
    
    /**
     * Validates the cron expression.
     * 
     * Validation Rules:
     * - Null or blank → VALID (optional field for one-time jobs)
     * - Valid Spring cron format → VALID
     * - Invalid format → INVALID
     * 
     * Interview Talking Point:
     * "I use Spring's CronExpression.parse() for validation because it's the same
     * parser used in JobController.calculateNextRunTime() and JobExecutor.calculateNextRunTimeFromCron().
     * This ensures validation consistency - if it passes validation here, it will
     * parse successfully during execution."
     * 
     * @param value the cron expression to validate
     * @param context the validation context
     * @return true if valid or null/blank, false if invalid
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null or blank values (optional field for one-time jobs)
        if (value == null || value.isBlank()) {
            log.debug("Cron expression is null or blank - validation passed (optional field)");
            return true;
        }
        
        try {
            // Parse using Spring's CronExpression
            CronExpression.parse(value);
            
            log.debug("Cron expression '{}' is valid", value);
            return true;
            
        } catch (IllegalArgumentException e) {
            // Invalid cron expression
            log.debug("Cron expression '{}' is invalid: {}", value, e.getMessage());
            
            // Optionally customize the error message with parsing details
            // This provides more helpful feedback to API clients
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Invalid cron expression format: " + e.getMessage()
            ).addConstraintViolation();
            
            return false;
        }
    }
}

