package com.logistics.inventory.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection: after MAX_ATTEMPTS failed logins for an email,
 * further attempts are rejected until the lockout window passes. Suitable for a
 * single-node deployment; swap for a shared store (Redis/DB) when scaling out.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCKOUT = Duration.ofMinutes(15);

    private record Attempts(int count, Instant firstFailure) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        Attempts current = attempts.get(normalize(email));
        if (current == null) return false;
        if (current.firstFailure().plus(LOCKOUT).isBefore(Instant.now())) {
            attempts.remove(normalize(email));
            return false;
        }
        return current.count() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String email) {
        attempts.merge(normalize(email), new Attempts(1, Instant.now()), (old, ignored) -> {
            // restart the window if the old one expired
            if (old.firstFailure().plus(LOCKOUT).isBefore(Instant.now())) {
                return new Attempts(1, Instant.now());
            }
            return new Attempts(old.count() + 1, old.firstFailure());
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(normalize(email));
    }

    public long minutesRemaining(String email) {
        Attempts current = attempts.get(normalize(email));
        if (current == null) return 0;
        long seconds = Duration.between(Instant.now(), current.firstFailure().plus(LOCKOUT)).getSeconds();
        return Math.max(1, seconds / 60);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
