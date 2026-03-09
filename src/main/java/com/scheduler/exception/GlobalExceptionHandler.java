package com.scheduler.exception;

import com.scheduler.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * 
 * Provides consistent error responses across all controllers.
 * 
 * Interview Talking Points:
 * - "@RestControllerAdvice centralizes exception handling across all controllers"
 * - "Maps domain exceptions to appropriate HTTP status codes"
 * - "Provides detailed validation errors for better client-side UX"
 * - "Logs errors for debugging while returning safe messages to clients"
 * 
 * Design Decision:
 * "I use @RestControllerAdvice instead of @ControllerAdvice because all our
 * endpoints return JSON. This automatically serializes ErrorResponse to JSON
 * without needing @ResponseBody on each handler method."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handles job not found exceptions.
     * 
     * @param ex Exception
     * @param request HTTP request
     * @return 404 Not Found response
     */
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(
            JobNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Job not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handles job execution not found exceptions.
     * 
     * @param ex Exception
     * @param request HTTP request
     * @return 404 Not Found response
     */
    @ExceptionHandler(JobExecutionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobExecutionNotFound(
            JobExecutionNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Job execution not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handles invalid job state transitions.
     * 
     * Interview Talking Point:
     * "State machine violations return 400 Bad Request because the client
     * attempted an invalid operation (e.g., starting an already running job)."
     * 
     * @param ex Exception
     * @param request HTTP request
     * @return 400 Bad Request response
     */
    @ExceptionHandler(InvalidJobStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJobState(
            InvalidJobStateException ex,
            HttpServletRequest request) {
        
        log.warn("Invalid job state transition: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handles illegal state exceptions.
     * 
     * @param ex Exception
     * @param request HTTP request
     * @return 400 Bad Request response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {
        
        log.warn("Illegal state: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles optimistic lock exceptions (concurrent modification).
     *
     * Interview Talking Point:
     * "Optimistic locking failures return 409 Conflict because another client
     * modified the resource. The client should retry with the latest version."
     *
     * @param ex Exception
     * @param request HTTP request
     * @return 409 Conflict response
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException ex,
            HttpServletRequest request) {

        log.warn("Optimistic lock exception: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            "Resource was modified by another request. Please retry with the latest version.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles validation errors from @Valid annotations.
     *
     * Interview Talking Point:
     * "Bean Validation errors are mapped to 400 Bad Request with detailed
     * field-level errors. This helps clients display validation messages
     * next to the appropriate form fields."
     *
     * @param ex Exception
     * @param request HTTP request
     * @return 400 Bad Request response with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed: {} errors", ex.getBindingResult().getErrorCount());

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.ValidationError(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
            ))
            .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation failed for " + validationErrors.size() + " field(s)",
            request.getRequestURI(),
            validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles all other exceptions.
     *
     * Interview Talking Point:
     * "Generic exception handler catches unexpected errors and returns 500.
     * We log the full stack trace for debugging but return a safe message
     * to clients to avoid leaking implementation details."
     *
     * @param ex Exception
     * @param request HTTP request
     * @return 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred. Please contact support if the problem persists.",
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

