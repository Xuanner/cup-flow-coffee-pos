package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cupflow.pos.TestcontainersConfiguration;
import com.cupflow.pos.auth.api.AuthController;
import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.PasswordHasher;
import com.cupflow.pos.auth.domain.RoleCode;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.logging.SecurityEventRecorder;
import com.cupflow.pos.shared.logging.TraceContext;
import com.cupflow.pos.shared.security.EndpointRole;
import com.cupflow.pos.shared.security.RequiresRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, SecurityEventIntegrationTest.ProbeConfiguration.class})
@SpringBootTest(
        properties = {
            "auth.bootstrap.enabled=true",
            "auth.bootstrap.cashier.username=audit-cashier",
            "auth.bootstrap.cashier.password=test-only-audit-cashier",
            "auth.bootstrap.cashier.display-name=审计收银员",
            "auth.bootstrap.admin.username=audit-admin",
            "auth.bootstrap.admin.password=test-only-audit-admin",
            "auth.bootstrap.admin.display-name=审计管理员",
            "auth.security.allowed-origins=http://localhost:5173",
            "auth.security.cookie-secure=false"
        })
@AutoConfigureMockMvc
class SecurityEventIntegrationTest {

    private static final String ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private SessionTokenIssuer tokenIssuer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Logger eventLogger = (Logger) LoggerFactory.getLogger(SecurityEventRecorder.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureSecurityEvents() {
        appender.start();
        eventLogger.addAppender(appender);
    }

    @AfterEach
    void stopCapturingSecurityEvents() {
        eventLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("TC-S2-AUDIT-001 至 003 登录成功、统一失败和限流产生可追踪脱敏事件")
    void recordsLoginSuccessFailureAndRateLimitWithoutCredentials() throws Exception {
        MvcResult success = login("audit-cashier", "test-only-audit-cashier", "192.0.2.101")
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> successEvent = onlyEvent("AUTHENTICATION_SUCCEEDED");
        assertThat(successEvent)
                .containsEntry("outcome", "SUCCEEDED")
                .containsEntry("target", "auth.login")
                .containsKey("accountId");
        assertTraceMatches(success, successEvent);

        clearEvents();
        MvcResult failure = login("unknown-audit", "test-only-audit-unknown", "192.0.2.102")
                .andExpect(status().isUnauthorized())
                .andReturn();
        Map<String, Object> failureEvent = onlyEvent("AUTHENTICATION_FAILED");
        assertThat(failureEvent).containsEntry("outcome", "DENIED").doesNotContainKey("accountId");
        assertTraceMatches(failure, failureEvent);

        String limitedUsername = "audit-limited-" + UUID.randomUUID();
        for (int attempt = 1; attempt <= 4; attempt++) {
            login(limitedUsername, "test-only-audit-limited", "192.0.2.103").andExpect(status().isUnauthorized());
        }
        clearEvents();
        MvcResult limited = login(limitedUsername, "test-only-audit-limited", "192.0.2.103")
                .andExpect(status().isTooManyRequests())
                .andReturn();
        Map<String, Object> limitedEvent = onlyEvent("AUTHENTICATION_RATE_LIMITED");
        assertThat(limitedEvent).containsEntry("outcome", "DENIED").doesNotContainKey("accountId");
        assertTraceMatches(limited, limitedEvent);
        assertCapturedLogsExclude(
                "audit-cashier", "unknown-audit", limitedUsername, "test-only-audit", "CUP_FLOW_SESSION");
    }

    @Test
    @DisplayName("TC-S2-AUDIT-004 至 006 会话过期、账号停用和退出产生最小事件")
    void recordsSessionExpiryAccountInvalidationAndLogout() throws Exception {
        String expiredToken = token(login("audit-cashier", "test-only-audit-cashier", "192.0.2.104")
                .andExpect(status().isOk())
                .andReturn());
        jdbcClient.sql("""
                        UPDATE auth_sessions
                        SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 hours',
                            idle_expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second',
                            last_activity_at = CURRENT_TIMESTAMP - INTERVAL '31 minutes'
                        WHERE token_hash = :tokenHash
                        """).param("tokenHash", tokenIssuer.hash(expiredToken)).update();
        clearEvents();
        MvcResult expired = mockMvc.perform(
                        get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, expiredToken)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        Map<String, Object> expiredEvent = onlyEvent("SESSION_EXPIRED");
        assertThat(expiredEvent)
                .containsEntry("outcome", "INVALIDATED")
                .containsEntry("reason", "IDLE_TIMEOUT")
                .containsKey("accountId");
        assertTraceMatches(expired, expiredEvent);

        String username = "audit-disabled-" + UUID.randomUUID();
        Account account = Account.newAccount(
                UUID.randomUUID(),
                new AccountUsername(username),
                passwordHasher.hash("test-only-audit-disabled"),
                "审计停用账号",
                AccountStatus.ACTIVE);
        accountRepository.insertIfAbsent(account);
        accountRepository.assignRoleIfAbsent(account.id(), RoleCode.CASHIER);
        String disabledToken = token(login(username, "test-only-audit-disabled", "192.0.2.105")
                .andExpect(status().isOk())
                .andReturn());
        jdbcClient
                .sql("UPDATE accounts SET status = 'DISABLED' WHERE id = :id")
                .param("id", account.id())
                .update();
        clearEvents();
        MvcResult disabled = mockMvc.perform(
                        get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, disabledToken)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        Map<String, Object> disabledEvent = onlyEvent("ACCOUNT_SESSION_INVALIDATED");
        assertThat(disabledEvent)
                .containsEntry("accountId", account.id())
                .containsEntry("reason", "ACCOUNT_UNAVAILABLE");
        assertTraceMatches(disabled, disabledEvent);

        String logoutToken = token(login("audit-cashier", "test-only-audit-cashier", "192.0.2.106")
                .andExpect(status().isOk())
                .andReturn());
        clearEvents();
        String csrf = csrfToken();
        clearEvents();
        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrf)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, logoutToken)))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> logoutEvent = onlyEvent("LOGOUT_SUCCEEDED");
        assertThat(logoutEvent)
                .containsEntry("outcome", "SUCCEEDED")
                .containsEntry("target", "auth.logout")
                .containsKey("accountId");
        assertTraceMatches(logout, logoutEvent);
        assertCapturedLogsExclude(expiredToken, disabledToken, logoutToken, "test-only-audit", "Cookie");
    }

    @Test
    @DisplayName("TC-S2-AUDIT-007/008 权限和 CSRF 拒绝产生可关联且不泄露规则的事件")
    void recordsAuthorizationAndCsrfDenialsWithoutSensitiveContext() throws Exception {
        String token = token(login("audit-cashier", "test-only-audit-cashier", "192.0.2.107")
                .andExpect(status().isOk())
                .andReturn());
        clearEvents();
        MvcResult forbidden = mockMvc.perform(
                        get("/api/v1/audit-probe/admin").cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isForbidden())
                .andReturn();
        Map<String, Object> authorizationEvent = onlyEvent("AUTHORIZATION_DENIED");
        assertThat(authorizationEvent)
                .containsEntry("outcome", "DENIED")
                .containsEntry("target", "/api/v1/audit-probe/admin")
                .containsKey("accountId")
                .doesNotContainKey("reason");
        assertTraceMatches(forbidden, authorizationEvent);

        clearEvents();
        MvcResult csrfDenied = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", "https://untrusted.example")
                        .header("X-XSRF-TOKEN", "test-only-invalid-csrf")
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isForbidden())
                .andReturn();
        Map<String, Object> csrfEvent = onlyEvent("CSRF_REJECTED");
        assertThat(csrfEvent)
                .containsEntry("outcome", "DENIED")
                .containsEntry("target", "/api/v1/auth/logout")
                .containsEntry("reason", "VALIDATION_FAILED")
                .doesNotContainKey("accountId");
        assertTraceMatches(csrfDenied, csrfEvent);
        assertCapturedLogsExclude(
                token,
                "test-only-audit-cashier",
                "test-only-invalid-csrf",
                "https://untrusted.example",
                "X-XSRF-TOKEN",
                "Cookie",
                "ADMIN",
                "CASHIER");
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String username, String password, String sourceAddress) throws Exception {
        String csrf = csrfToken();
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(sourceAddress);
                    return request;
                })
                .header("Origin", ORIGIN)
                .header("X-XSRF-TOKEN", csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Credentials(username, password))));
    }

    private String csrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.path("data").path("token").asText();
    }

    private String token(MvcResult result) {
        return result.getResponse().getCookie(AuthController.SESSION_COOKIE).getValue();
    }

    private Map<String, Object> onlyEvent(String eventType) {
        List<ILoggingEvent> matches = appender.list.stream()
                .filter(event -> eventType.equals(fields(event).get("securityEvent")))
                .toList();
        assertThat(matches).hasSize(1);
        return fields(matches.getFirst());
    }

    private Map<String, Object> fields(ILoggingEvent event) {
        Map<String, Object> fields = new HashMap<>();
        event.getKeyValuePairs().forEach(pair -> fields.put(pair.key, pair.value));
        event.getMDCPropertyMap().forEach(fields::putIfAbsent);
        return fields;
    }

    private void assertTraceMatches(MvcResult response, Map<String, Object> event) throws Exception {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsByteArray());
        assertThat(event)
                .containsKeys("eventTime", "traceId")
                .containsEntry("traceId", body.path("traceId").asText());
    }

    private void assertCapturedLogsExclude(String... sensitiveValues) {
        Map<String, String> artifacts = new HashMap<>();
        for (int index = 0; index < appender.list.size(); index++) {
            ILoggingEvent event = appender.list.get(index);
            assertThat(event.getThrowableProxy()).isNull();
            artifacts.put("security-event-" + index, event.getFormattedMessage() + fields(event));
        }
        SensitiveValueScanner.assertAbsent(artifacts, List.of(sensitiveValues));
    }

    private void clearEvents() {
        appender.list.clear();
    }

    record Credentials(String username, String password) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        AuditProbeController auditProbeController() {
            return new AuditProbeController();
        }
    }

    @RestController
    @RequestMapping("/api/v1/audit-probe")
    static class AuditProbeController {

        @GetMapping("/admin")
        @RequiresRole(EndpointRole.ADMIN)
        ApiResponse<String> admin() {
            return ApiResponse.success("allowed", TraceContext.currentTraceId());
        }
    }
}
