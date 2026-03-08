package com.scheduler.coordination;

import com.scheduler.domain.entity.SchedulerNode;
import com.scheduler.repository.SchedulerNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Provides fencing tokens to prevent split-brain scenarios.
 * 
 * Fencing tokens are monotonically increasing identifiers (epoch numbers)
 * that ensure only the current leader can perform writes. When a new leader
 * is elected, its epoch is incremented, invalidating any writes from the
 * previous leader.
 * 
 * Interview Talking Points:
 * - "Fencing tokens solve the split-brain problem where two nodes think they're leader"
 * - "Each leader has a unique epoch number that's monotonically increasing"
 * - "The database validates that writes come from the current epoch"
 * - "This prevents zombie leaders from corrupting state after network partitions"
 * - "Even if Redis has a split-brain, the database rejects writes from stale epochs"
 * 
 * Example Scenario:
 * 1. Node A is leader with epoch 5
 * 2. Network partition occurs
 * 3. Node B becomes leader with epoch 6
 * 4. Node A recovers and tries to write with epoch 5
 * 5. Database rejects the write because epoch 6 > epoch 5
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FencingTokenProvider {
    
    private final SchedulerNodeRepository nodeRepository;
    private final LeaderElectionService leaderElectionService;
    
    /**
     * Gets the current fencing token for the leader.
     * 
     * The fencing token is the epoch number of the current leader.
     * 
     * @return Optional containing the fencing token, or empty if no leader exists
     */
    public Optional<Long> getCurrentFencingToken() {
        try {
            Optional<SchedulerNode> leader = nodeRepository.findCurrentLeader();
            
            if (leader.isPresent()) {
                long epoch = leader.get().getEpoch();
                log.debug("Current fencing token (epoch): {}", epoch);
                return Optional.of(epoch);
            } else {
                log.debug("No current leader - no fencing token available");
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting current fencing token", e);
            return Optional.empty();
        }
    }
    
    /**
     * Gets the fencing token for this node.
     * 
     * Returns the epoch number if this node is the leader, otherwise empty.
     * 
     * @return Optional containing this node's fencing token
     */
    public Optional<Long> getMyFencingToken() {
        try {
            if (leaderElectionService.isLeader()) {
                long epoch = leaderElectionService.getCurrentEpoch();
                log.debug("My fencing token (epoch): {}", epoch);
                return Optional.of(epoch);
            } else {
                log.debug("Not the leader - no fencing token");
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting my fencing token", e);
            return Optional.empty();
        }
    }
    
    /**
     * Validates that a fencing token is current.
     * 
     * A token is valid if it matches the current leader's epoch.
     * 
     * @param token the fencing token to validate
     * @return true if the token is valid
     */
    public boolean isTokenValid(long token) {
        try {
            Optional<Long> currentToken = getCurrentFencingToken();
            
            if (currentToken.isPresent()) {
                boolean valid = currentToken.get().equals(token);
                
                if (!valid) {
                    log.warn("Invalid fencing token: {} (current: {})", token, currentToken.get());
                }
                
                return valid;
            } else {
                log.warn("Cannot validate token {} - no current leader", token);
                return false;
            }
        } catch (Exception e) {
            log.error("Error validating fencing token: {}", token, e);
            return false;
        }
    }
    
    /**
     * Validates that a fencing token is not stale.
     * 
     * A token is stale if it's less than the current epoch.
     * This prevents zombie leaders from performing writes.
     * 
     * @param token the fencing token to check
     * @return true if the token is stale
     */
    public boolean isTokenStale(long token) {
        try {
            Optional<Long> currentToken = getCurrentFencingToken();
            
            if (currentToken.isPresent()) {
                boolean stale = token < currentToken.get();
                
                if (stale) {
                    log.warn("Stale fencing token detected: {} (current: {})", token, currentToken.get());
                }
                
                return stale;
            } else {
                // No current leader, so we can't determine if token is stale
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking if token {} is stale", token, e);
            return false;
        }
    }
    
    /**
     * Generates a formatted fencing token string.
     *
     * Format: "epoch{N}-node{ID}"
     *
     * @param nodeId the node ID
     * @param epoch the epoch number
     * @return formatted fencing token
     */
    public String formatFencingToken(String nodeId, long epoch) {
        return String.format("epoch%d-node%s", epoch, nodeId);
    }

    /**
     * Validates a fencing token string against the current leader's epoch.
     *
     * Extracts the epoch from the token format "epoch{N}-node{ID}" and
     * compares it with the current leader's epoch.
     *
     * Interview Talking Point:
     * "I validate fencing tokens on every job status update to prevent zombie
     * executions from corrupting state. Even if a node loses its distributed
     * lock, it can't update the job status because its fencing token is stale."
     *
     * @param fencingToken the fencing token string to validate
     * @return true if the token matches the current leader's epoch
     */
    public boolean isTokenValid(String fencingToken) {
        if (fencingToken == null || fencingToken.isEmpty()) {
            log.warn("Cannot validate null or empty fencing token");
            return false;
        }

        long epoch = extractEpochFromToken(fencingToken);
        if (epoch == -1) {
            log.warn("Invalid fencing token format: {}", fencingToken);
            return false;
        }

        return isTokenValid(epoch);
    }

    /**
     * Checks if a fencing token string is stale.
     *
     * @param fencingToken the fencing token string to check
     * @return true if the token is from a previous epoch
     */
    public boolean isTokenStale(String fencingToken) {
        if (fencingToken == null || fencingToken.isEmpty()) {
            return true;
        }

        long epoch = extractEpochFromToken(fencingToken);
        if (epoch == -1) {
            return true;
        }

        return isTokenStale(epoch);
    }

    /**
     * Gets the current valid fencing token as a formatted string.
     *
     * @return Optional containing the current fencing token string
     */
    public Optional<String> getCurrentFencingTokenString() {
        try {
            Optional<SchedulerNode> leader = nodeRepository.findCurrentLeader();

            if (leader.isPresent()) {
                SchedulerNode leaderNode = leader.get();
                String token = formatFencingToken(leaderNode.getNodeId(), leaderNode.getEpoch());
                log.debug("Current fencing token string: {}", token);
                return Optional.of(token);
            } else {
                log.debug("No current leader - no fencing token available");
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting current fencing token string", e);
            return Optional.empty();
        }
    }

    /**
     * Extracts the epoch number from a fencing token string.
     *
     * Format: "epoch{N}-node{ID}"
     *
     * @param fencingToken the fencing token string
     * @return the epoch number, or -1 if invalid format
     */
    public long extractEpochFromToken(String fencingToken) {
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
            log.error("Error extracting epoch from token: {}", fencingToken, e);
            return -1;
        }
    }

    /**
     * Extracts the node ID from a fencing token string.
     *
     * Format: "epoch{N}-node{ID}"
     *
     * @param fencingToken the fencing token string
     * @return the node ID, or null if invalid format
     */
    public String extractNodeIdFromToken(String fencingToken) {
        if (fencingToken == null || !fencingToken.contains("-node")) {
            return null;
        }
        try {
            int nodeIndex = fencingToken.indexOf("-node") + 5;
            return fencingToken.substring(nodeIndex);
        } catch (Exception e) {
            log.error("Error extracting node ID from token: {}", fencingToken, e);
            return null;
        }
    }
}

