package com.scheduler.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Bean Validation annotation for validating cron expressions.
 * 
 * Uses Spring's {@link org.springframework.scheduling.support.CronExpression}
 * for validation to ensure consistency with the cron parsing logic used
 * throughout the application.
 * 
 * Interview Talking Points:
 * - "I created a custom Bean Validation annotation to validate cron expressions
 *   at the API boundary, preventing invalid schedules from being persisted"
 * - "The validator uses Spring's CronExpression parser for consistency with
 *   the scheduling logic in JobController and JobExecutor"
 * - "Null values are allowed since cron expressions are optional for one-time jobs"
 * - "This follows JSR-380 Bean Validation standards for declarative validation"
 * 
 * Design Decision:
 * "I chose to create a custom validator instead of using regex because:
 * 1. Cron expressions have complex rules that are difficult to express in regex
 * 2. Spring's CronExpression parser provides accurate validation
 * 3. Using the same parser for validation and execution ensures consistency
 * 4. The validator can provide detailed error messages for specific parsing failures"
 * 
 * Usage Example:
 * <pre>
 * public record CreateJobRequest(
 *     &#64;ValidCronExpression
 *     String cronExpression
 * ) {}
 * </pre>
 * 
 * Valid Examples:
 * - 0 0 0 * * * (Daily at midnight)
 * - 0 0 * * * * (Every hour)
 * - 0 *&#47;5 * * * * (Every 5 minutes)
 * - null (Allowed for one-time jobs)
 * - blank (Allowed for one-time jobs)
 *
 * Invalid Examples:
 * - INVALID (Not a valid cron format)
 * - 0 0 0 * * (Missing field - needs 6 fields)
 * - 60 0 0 * * * (Invalid second value - max 59)
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 * @see CronExpressionValidator
 * @see org.springframework.scheduling.support.CronExpression
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CronExpressionValidator.class)
@Documented
public @interface ValidCronExpression {
    
    /**
     * Error message to display when validation fails.
     * 
     * Can be overridden in the annotation usage:
     * <pre>
     * &#64;ValidCronExpression(message = "Custom error message")
     * String cronExpression;
     * </pre>
     * 
     * @return the error message
     */
    String message() default "Invalid cron expression format";
    
    /**
     * Validation groups for conditional validation.
     * 
     * Allows grouping validations for different scenarios:
     * <pre>
     * &#64;ValidCronExpression(groups = CreateGroup.class)
     * String cronExpression;
     * </pre>
     * 
     * @return the validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for clients to assign custom metadata.
     * 
     * Used by validation clients to assign custom severity levels or metadata:
     * <pre>
     * &#64;ValidCronExpression(payload = Severity.Error.class)
     * String cronExpression;
     * </pre>
     * 
     * @return the payload classes
     */
    Class<? extends Payload>[] payload() default {};
}

