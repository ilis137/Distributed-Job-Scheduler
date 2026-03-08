package com.scheduler.exception;

/**
 * Exception thrown when a stale or zombie execution attempts to update job status.
 * 
 * This occurs when:
 * - A node loses its distributed lock but continues executing
 * - A node loses leadership but tries to update job status
 * - A node's fencing token is from a previous epoch
 * 
 * Interview Talking Point:
 * "I use fencing tokens to prevent zombie leaders from corrupting state after
 * network partitions or lock expiration. Each execution has an epoch number,
 * and only the current epoch can update job status. This is the same pattern
 * used by Google Chubby and Apache Kafka."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public class StaleExecutionException extends SchedulerException {

    private final String staleFencingToken;
    private final String currentFencingToken;
    private final Long jobId;

    /**
     * Creates a new StaleExecutionException.
     * 
     * @param jobId the job ID
     * @param staleFencingToken the stale fencing token that was rejected
     * @param currentFencingToken the current valid fencing token
     */
    public StaleExecutionException(Long jobId, String staleFencingToken, String currentFencingToken) {
        super(String.format(
            "Stale execution detected for job %d. Stale token: %s, Current token: %s. " +
            "This execution lost leadership or its distributed lock expired.",
            jobId, staleFencingToken, currentFencingToken
        ));
        this.jobId = jobId;
        this.staleFencingToken = staleFencingToken;
        this.currentFencingToken = currentFencingToken;
    }

    /**
     * Creates a new StaleExecutionException with custom message.
     * 
     * @param jobId the job ID
     * @param staleFencingToken the stale fencing token
     * @param currentFencingToken the current valid fencing token
     * @param message custom error message
     */
    public StaleExecutionException(Long jobId, String staleFencingToken, String currentFencingToken, String message) {
        super(message);
        this.jobId = jobId;
        this.staleFencingToken = staleFencingToken;
        this.currentFencingToken = currentFencingToken;
    }

    public String getStaleFencingToken() {
        return staleFencingToken;
    }

    public String getCurrentFencingToken() {
        return currentFencingToken;
    }

    public Long getJobId() {
        return jobId;
    }

    /**
     * Extracts the epoch number from a fencing token.
     * 
     * @param fencingToken the fencing token (format: "epoch{N}-node{ID}")
     * @return the epoch number, or -1 if invalid format
     */
    public static long extractEpoch(String fencingToken) {
        if (fencingToken == null || !fencingToken.startsWith("epoch")) {
            return -1;
        }
        try {
            int dashIndex = fencingToken.indexOf("-node");
            if (dashIndex == -1) {
                return -1;
            }
            String epochPart = fencingToken.substring(5, dashIndex);
            return Long.parseLong(epochPart);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Gets the epoch difference between stale and current tokens.
     * 
     * @return the number of epochs behind (positive if stale, 0 if same, negative if ahead)
     */
    public long getEpochDifference() {
        long staleEpoch = extractEpoch(staleFencingToken);
        long currentEpoch = extractEpoch(currentFencingToken);
        return currentEpoch - staleEpoch;
    }
}

