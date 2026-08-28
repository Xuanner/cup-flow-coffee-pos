package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.AccountUsername;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptLimiter {

    static final int FAILURE_LIMIT = 5;
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentMap<AttemptKey, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public OptionalLong retryAfter(String sourceAddress, AccountUsername username) {
        AttemptKey key = AttemptKey.of(sourceAddress, username);
        Instant now = clock.instant();
        AttemptState state = attempts.get(key);
        if (state == null) {
            return OptionalLong.empty();
        }
        if (state.isBlockedAt(now)) {
            return OptionalLong.of(secondsUntil(now, state.blockedUntil()));
        }
        if (state.isExpiredAt(now)) {
            attempts.remove(key, state);
        }
        return OptionalLong.empty();
    }

    public OptionalLong recordFailure(String sourceAddress, AccountUsername username) {
        AttemptKey key = AttemptKey.of(sourceAddress, username);
        Instant now = clock.instant();
        AtomicReference<OptionalLong> decision = new AtomicReference<>(OptionalLong.empty());
        attempts.compute(key, (ignored, current) -> {
            if (current != null && current.isBlockedAt(now)) {
                decision.set(OptionalLong.of(secondsUntil(now, current.blockedUntil())));
                return current;
            }
            int failures = current == null || current.isExpiredAt(now) ? 1 : current.failures() + 1;
            Instant firstFailureAt = failures == 1 ? now : current.firstFailureAt();
            if (failures >= FAILURE_LIMIT) {
                Instant blockedUntil = now.plus(BLOCK_DURATION);
                decision.set(OptionalLong.of(secondsUntil(now, blockedUntil)));
                return new AttemptState(failures, firstFailureAt, blockedUntil);
            }
            return new AttemptState(failures, firstFailureAt, null);
        });
        return decision.get();
    }

    public void clear(String sourceAddress, AccountUsername username) {
        attempts.remove(AttemptKey.of(sourceAddress, username));
    }

    private long secondsUntil(Instant now, Instant deadline) {
        long milliseconds = Duration.between(now, deadline).toMillis();
        return Math.max(1, Math.floorDiv(milliseconds + 999, 1000));
    }

    private record AttemptKey(String sourceAddress, String username) {
        static AttemptKey of(String sourceAddress, AccountUsername username) {
            String source = sourceAddress == null || sourceAddress.isBlank() ? "unknown" : sourceAddress;
            return new AttemptKey(source, username.value());
        }
    }

    private record AttemptState(int failures, Instant firstFailureAt, Instant blockedUntil) {
        boolean isBlockedAt(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        boolean isExpiredAt(Instant now) {
            Instant deadline = blockedUntil != null ? blockedUntil : firstFailureAt.plus(FAILURE_WINDOW);
            return !now.isBefore(deadline);
        }
    }
}
