package com.cupflow.pos.auth.application;

import com.cupflow.pos.auth.domain.AuthSessionRepository;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SessionCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleanupService.class);
    private static final Duration RETENTION = Duration.ofDays(7);

    private final AuthSessionRepository sessionRepository;
    private final Clock clock;

    public SessionCleanupService(AuthSessionRepository sessionRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${auth.security.session-cleanup-interval:PT1H}",
            initialDelayString = "${auth.security.session-cleanup-initial-delay:PT1H}")
    public void cleanup() {
        try {
            int deleted = sessionRepository.deleteInvalidBefore(clock.instant().minus(RETENTION));
            LOGGER.info("Authentication session cleanup removed {} retained records", deleted);
        } catch (RuntimeException exception) {
            LOGGER.error("Authentication session cleanup failed; runtime validation remains active");
        }
    }
}
