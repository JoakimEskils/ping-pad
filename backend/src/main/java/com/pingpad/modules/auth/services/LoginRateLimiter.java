package com.pingpad.modules.auth.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter for login attempts.
 * Limits to 5 attempts per minute per IP address.
 */
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60; // 1 minute

    // Map of IP address -> list of attempt timestamps
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public LoginRateLimiter() {
        // Clean up old entries every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Check if the IP address has exceeded the rate limit
     * @param ipAddress The IP address to check
     * @return true if rate limit is exceeded, false otherwise
     */
    public boolean isRateLimited(String ipAddress) {
        AttemptWindow window = attempts.computeIfAbsent(ipAddress, k -> new AttemptWindow());
        return window.isRateLimited();
    }

    /**
     * Record a login attempt for the given IP address
     * @param ipAddress The IP address that made the attempt
     */
    public void recordAttempt(String ipAddress) {
        AttemptWindow window = attempts.computeIfAbsent(ipAddress, k -> new AttemptWindow());
        window.recordAttempt();
    }

    /**
     * Get the number of remaining attempts for the IP address
     * @param ipAddress The IP address to check
     * @return Number of remaining attempts (0 if rate limited)
     */
    public int getRemainingAttempts(String ipAddress) {
        AttemptWindow window = attempts.get(ipAddress);
        if (window == null) {
            return MAX_ATTEMPTS;
        }
        return window.getRemainingAttempts();
    }

    /**
     * Get the time until the rate limit resets (in seconds)
     * @param ipAddress The IP address to check
     * @return Seconds until reset, or 0 if not rate limited
     */
    public long getSecondsUntilReset(String ipAddress) {
        AttemptWindow window = attempts.get(ipAddress);
        if (window == null || !window.isRateLimited()) {
            return 0;
        }
        return window.getSecondsUntilReset();
    }

    /**
     * Clear attempts for an IP address (useful after successful login)
     * @param ipAddress The IP address to clear
     */
    public void clearAttempts(String ipAddress) {
        attempts.remove(ipAddress);
    }

    /**
     * Clean up old entries that are no longer rate limited
     */
    private void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(WINDOW_SECONDS);
        attempts.entrySet().removeIf(entry -> {
            AttemptWindow window = entry.getValue();
            window.removeOldAttempts(cutoff);
            // Remove if no longer rate limited and no recent attempts
            return !window.isRateLimited() && window.getAttemptCount() == 0;
        });
    }

    /**
     * Inner class to track attempts within a time window
     */
    private static class AttemptWindow {
        private final java.util.concurrent.ConcurrentLinkedQueue<LocalDateTime> timestamps = 
            new java.util.concurrent.ConcurrentLinkedQueue<>();

        public synchronized boolean isRateLimited() {
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(WINDOW_SECONDS);
            removeOldAttempts(cutoff);
            return timestamps.size() >= MAX_ATTEMPTS;
        }

        public synchronized void recordAttempt() {
            LocalDateTime now = LocalDateTime.now();
            timestamps.offer(now);
            // Keep only recent attempts
            LocalDateTime cutoff = now.minusSeconds(WINDOW_SECONDS);
            removeOldAttempts(cutoff);
        }

        public synchronized int getRemainingAttempts() {
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(WINDOW_SECONDS);
            removeOldAttempts(cutoff);
            int currentAttempts = timestamps.size();
            return Math.max(0, MAX_ATTEMPTS - currentAttempts);
        }

        public synchronized long getSecondsUntilReset() {
            if (timestamps.isEmpty()) {
                return 0;
            }
            LocalDateTime oldest = timestamps.peek();
            if (oldest == null) {
                return 0;
            }
            LocalDateTime resetTime = oldest.plusSeconds(WINDOW_SECONDS);
            long seconds = java.time.Duration.between(LocalDateTime.now(), resetTime).getSeconds();
            return Math.max(0, seconds);
        }

        public synchronized int getAttemptCount() {
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(WINDOW_SECONDS);
            removeOldAttempts(cutoff);
            return timestamps.size();
        }

        private void removeOldAttempts(LocalDateTime cutoff) {
            while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
                timestamps.poll();
            }
        }
    }
}
