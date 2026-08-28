package com.cupflow.pos.auth.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cupflow.pos.auth.domain.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SessionCleanupServiceTest {

    @Test
    @DisplayName("TC-S2-SESS-016 清理失败不传播且不改变即时会话判断")
    void isolatesCleanupFailure() {
        AuthSessionRepository repository = mock(AuthSessionRepository.class);
        Instant now = Instant.parse("2026-08-28T05:00:00Z");
        Instant cutoff = now.minusSeconds(7 * 24 * 60 * 60L);
        when(repository.deleteInvalidBefore(cutoff)).thenThrow(new IllegalStateException("test-only database outage"));
        SessionCleanupService service = new SessionCleanupService(repository, Clock.fixed(now, ZoneOffset.UTC));

        assertThatCode(service::cleanup).doesNotThrowAnyException();
        verify(repository).deleteInvalidBefore(cutoff);
    }
}
