package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.auth.domain.AccountUsername;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private static final String SOURCE = "192.0.2.10";
    private static final AccountUsername USERNAME = new AccountUsername("cashier");

    private MutableClock clock;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-27T08:00:00Z"));
        limiter = new LoginAttemptLimiter(clock);
    }

    @Test
    @DisplayName("TC-S2-RATE-001 同组合前 4 次失败不进入限制")
    void allowsFirstFourFailures() {
        for (int attempt = 1; attempt < LoginAttemptLimiter.FAILURE_LIMIT; attempt++) {
            assertThat(limiter.recordFailure(SOURCE, USERNAME)).isEmpty();
            assertThat(limiter.retryAfter(SOURCE, USERNAME)).isEmpty();
        }
    }

    @Test
    @DisplayName("TC-S2-RATE-002 同组合第 5 次失败启动 15 分钟限制")
    void blocksOnFifthFailure() {
        recordFailures(LoginAttemptLimiter.FAILURE_LIMIT - 1);

        assertThat(limiter.recordFailure(SOURCE, USERNAME)).hasValue(900);
        assertThat(limiter.retryAfter(SOURCE, USERNAME)).hasValue(900);
    }

    @Test
    @DisplayName("TC-S2-RATE-003 限制期间继续尝试不延长截止时间")
    void doesNotExtendAnActiveBlock() {
        recordFailures(LoginAttemptLimiter.FAILURE_LIMIT);
        clock.advance(Duration.ofMinutes(1));

        assertThat(limiter.recordFailure(SOURCE, USERNAME)).hasValue(840);
        clock.advance(Duration.ofMinutes(14));
        assertThat(limiter.retryAfter(SOURCE, USERNAME)).isEmpty();
    }

    @Test
    @DisplayName("TC-S2-RATE-004 限制期结束后自动恢复")
    void automaticallyRecoversAfterBlockExpires() {
        recordFailures(LoginAttemptLimiter.FAILURE_LIMIT);
        clock.advance(LoginAttemptLimiter.BLOCK_DURATION);

        assertThat(limiter.retryAfter(SOURCE, USERNAME)).isEmpty();
        assertThat(limiter.recordFailure(SOURCE, USERNAME)).isEmpty();
    }

    @Test
    @DisplayName("TC-S2-RATE-005 来源与账号标识组合相互隔离")
    void isolatesSourceAndUsernameCombinations() {
        recordFailures(LoginAttemptLimiter.FAILURE_LIMIT);

        assertThat(limiter.retryAfter("192.0.2.11", USERNAME)).isEmpty();
        assertThat(limiter.retryAfter(SOURCE, new AccountUsername("admin"))).isEmpty();
    }

    @Test
    @DisplayName("TC-S2-RATE-006 成功登录清除对应失败状态")
    void clearsFailuresAfterSuccessfulLogin() {
        recordFailures(LoginAttemptLimiter.FAILURE_LIMIT - 1);

        limiter.clear(SOURCE, USERNAME);

        assertThat(limiter.recordFailure(SOURCE, USERNAME)).isEmpty();
    }

    private void recordFailures(int count) {
        for (int attempt = 0; attempt < count; attempt++) {
            limiter.recordFailure(SOURCE, USERNAME);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
