package com.cupflow.pos.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cupflow.pos.TestcontainersConfiguration;
import com.cupflow.pos.auth.domain.Account;
import com.cupflow.pos.auth.domain.AccountRepository;
import com.cupflow.pos.auth.domain.AccountStatus;
import com.cupflow.pos.auth.domain.AccountUsername;
import com.cupflow.pos.auth.domain.PasswordHasher;
import com.cupflow.pos.auth.domain.RoleCode;
import com.cupflow.pos.auth.domain.SessionTokenIssuer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "auth.bootstrap.enabled=true",
            "auth.bootstrap.cashier.username=login-cashier",
            "auth.bootstrap.cashier.password=test-only-cashier-login",
            "auth.bootstrap.cashier.display-name=登录收银员",
            "auth.bootstrap.admin.username=login-admin",
            "auth.bootstrap.admin.password=test-only-admin-login",
            "auth.bootstrap.admin.display-name=登录管理员",
            "auth.security.allowed-origins=http://localhost:5173",
            "auth.security.cookie-secure=false"
        })
@AutoConfigureMockMvc
class AuthLoginIntegrationTest {

    private static final String ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private SessionTokenIssuer tokenIssuer;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    @DisplayName("TC-S2-AUTH-001 有效 CASHIER 登录返回最小用户并设置安全会话 Cookie")
    void cashierCanLoginWithSafeCookieAndMinimalResponse() throws Exception {
        MvcResult result = login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("登录收银员"))
                .andExpect(jsonPath("$.data.roles[0]").value("CASHIER"))
                .andExpect(jsonPath("$.data.defaultPath").value("/pos"))
                .andExpect(jsonPath("$.data.username").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(cookie().path(AuthController.SESSION_COOKIE, "/"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie)
                .contains("SameSite=Lax", "HttpOnly", "Path=/")
                .doesNotContain("Max-Age", "Expires", "test-only-cashier-login");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("login-cashier", "test-only-cashier-login", "passwordHash", "sessionToken");
    }

    @Test
    @DisplayName("TC-S2-AUTH-002 有效 ADMIN 登录返回管理员默认页")
    void adminCanLoginAndUsesDashboardAsDefaultPath() throws Exception {
        login("login-admin", "test-only-admin-login", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("登录管理员"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.defaultPath").value("/dashboard"));
    }

    @Test
    @DisplayName("TC-S2-AUTH-003 username 去除首尾空白后校验")
    void trimsUsernameBeforeAuthentication() throws Exception {
        login("  login-cashier  ", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultPath").value("/pos"));
    }

    @Test
    @DisplayName("TC-S2-AUTH-004 password 保持首尾空白和原始内容")
    void preservesPasswordWithoutNormalization() throws Exception {
        String username = "whitespace-password-" + UUID.randomUUID();
        Account account = Account.newAccount(
                UUID.randomUUID(),
                new AccountUsername(username),
                passwordHasher.hash(" test-only-space-password "),
                "空白密码测试账号",
                AccountStatus.ACTIVE);
        accountRepository.insertIfAbsent(account);
        accountRepository.assignRoleIfAbsent(account.id(), RoleCode.CASHIER);

        login(username, "test-only-space-password", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-002"));
        login(username, " test-only-space-password ", null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-S2-AUTH-005 空字段和超长字段返回字段错误且不创建会话")
    void rejectsInvalidFieldsBeforeAuthentication() throws Exception {
        long before = sessionCount();
        String csrfToken = csrfToken();

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"   ","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400-001"))
                .andExpect(jsonPath("$.data").isArray());

        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-AUTH-009 登录前旧会话在成功登录后被轮换")
    void replacesAnExistingSessionWithANewCredential() throws Exception {
        MvcResult first = login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andReturn();
        String firstToken = cookieValue(first);

        MvcResult second = login("login-cashier", "test-only-cashier-login", firstToken)
                .andExpect(status().isOk())
                .andReturn();
        String secondToken = cookieValue(second);

        assertThat(secondToken).isNotEqualTo(firstToken);
        String reason = jdbcClient
                .sql("SELECT revocation_reason FROM auth_sessions WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenIssuer.hash(firstToken))
                .query(String.class)
                .single();
        assertThat(reason).isEqualTo("REPLACED");
    }

    @Test
    @DisplayName("TC-S2-AUTH-010 缺失或错误 CSRF 时拒绝登录且不校验密码")
    void rejectsMissingOrInvalidCsrfBeforeCreatingASession() throws Exception {
        long before = sessionCount();
        String request = """
                {"username":"login-cashier","password":"test-only-cashier-login"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-002"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", "invalid-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-002"));

        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-AUTH-006 至 008 不存在、错误密码和停用账号使用统一失败响应")
    void returnsTheSameFailureForUnknownWrongPasswordAndDisabledAccount() throws Exception {
        String disabledUsername = "disabled-login-" + UUID.randomUUID();
        Account disabled = Account.newAccount(
                UUID.randomUUID(),
                new AccountUsername(disabledUsername),
                passwordHasher.hash("test-only-disabled-login"),
                "停用登录测试账号",
                AccountStatus.DISABLED);
        accountRepository.insertIfAbsent(disabled);
        accountRepository.assignRoleIfAbsent(disabled.id(), RoleCode.CASHIER);
        long before = sessionCount();

        assertUnifiedAuthenticationFailure("unknown-" + UUID.randomUUID(), "test-only-unknown-login", "192.0.2.21");
        assertUnifiedAuthenticationFailure("login-cashier", "test-only-wrong-login", "192.0.2.22");
        assertUnifiedAuthenticationFailure(disabledUsername, "test-only-disabled-login", "192.0.2.23");

        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-RATE-001 至 003 第 5 次失败返回 429 且限制期间不延长")
    void rateLimitsTheFifthFailureWithoutTrustingForwardedFor() throws Exception {
        String username = "rate-limit-" + UUID.randomUUID();
        String sourceAddress = "192.0.2.30";
        long before = sessionCount();

        for (int attempt = 1; attempt <= 4; attempt++) {
            login(username, "test-only-rate-limit", null, sourceAddress, "198.51.100." + attempt)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-401-002"));
        }

        MvcResult limited = login(username, "test-only-rate-limit", null, sourceAddress, "198.51.100.5")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.code").value("AUTH-429-001"))
                .andExpect(jsonPath("$.message").value("尝试次数过多，请稍后再试"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andReturn();
        login(username, "test-only-rate-limit", null, sourceAddress, "203.0.113.99")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));

        assertThat(limited.getResponse().getContentAsString())
                .doesNotContain(
                        "test-only-rate-limit",
                        "password",
                        "passwordHash",
                        "sessionToken",
                        "CUP_FLOW_SESSION",
                        "Exception");
        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-RATE-006 成功登录清除来源与账号组合的失败状态")
    void successfulLoginClearsPreviousFailures() throws Exception {
        String sourceAddress = "192.0.2.31";
        for (int attempt = 1; attempt <= 4; attempt++) {
            login("login-cashier", "test-only-wrong-login", null, sourceAddress, null)
                    .andExpect(status().isUnauthorized());
        }

        login("login-cashier", "test-only-cashier-login", null, sourceAddress, null)
                .andExpect(status().isOk());

        login("login-cashier", "test-only-wrong-login", null, sourceAddress, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-002"));
    }

    @Test
    @DisplayName("TC-S2-SESS-001 有效 Cookie 查询当前身份并刷新活动时间")
    void returnsCurrentUserForAValidSessionWithoutExposingCredentials() throws Exception {
        MvcResult login = login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andReturn();
        String rawToken = cookieValue(login);
        String tokenHash = tokenIssuer.hash(rawToken);
        Instant activityBefore = jdbcClient
                .sql("SELECT last_activity_at FROM auth_sessions WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .query(Instant.class)
                .single();

        MvcResult result = mockMvc.perform(
                        get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.displayName").value("登录收银员"))
                .andExpect(jsonPath("$.data.roles[0]").value("CASHIER"))
                .andExpect(jsonPath("$.data.defaultPath").value("/pos"))
                .andExpect(jsonPath("$.data.username").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        Instant activityAfter = jdbcClient
                .sql("SELECT last_activity_at FROM auth_sessions WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .query(Instant.class)
                .single();
        assertThat(activityAfter).isAfterOrEqualTo(activityBefore);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(rawToken, tokenHash, "sessionToken", "passwordHash", "login-cashier");
    }

    @Test
    @DisplayName("TC-S2-SESS-002 无 Cookie 查询当前身份返回 401 且不创建会话")
    void rejectsMissingSessionWithoutCreatingOne() throws Exception {
        long before = sessionCount();

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-001"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-SESS-003 伪造 Cookie 返回 401、清除 Cookie 且不泄露原因")
    void rejectsAndClearsAForgedSessionWithoutCreatingOne() throws Exception {
        long before = sessionCount();
        String forgedToken = "A".repeat(43);

        MvcResult result = mockMvc.perform(
                        get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, forgedToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-001"))
                .andExpect(jsonPath("$.message").value("登录状态已失效，请重新登录"))
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0))
                .andExpect(cookie().httpOnly(AuthController.SESSION_COOKIE, true))
                .andExpect(cookie().path(AuthController.SESSION_COOKIE, "/"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("SameSite=Lax", "Max-Age=0")
                .doesNotContain(forgedToken);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(forgedToken, "tokenHash", "sessionToken", "Cookie", "Exception");
        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-SESS-014 至 015 合法 Origin 通过且恶意 Origin 被 CSRF 防护拒绝")
    void enforcesOriginAlongsideCsrfForStateChangingRequests() throws Exception {
        login("login-cashier", "test-only-cashier-login", null).andExpect(status().isOk());
        long before = sessionCount();
        String csrfToken = csrfToken();

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "https://evil.example")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"login-cashier","password":"test-only-cashier-login"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-002"));

        assertThat(sessionCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("TC-S2-SESS-005/007 空闲或绝对过期返回 401、撤销并清 Cookie")
    void rejectsAndRevokesIdleAndAbsoluteExpiredSessions() throws Exception {
        String idleToken = cookieValue(login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andReturn());
        expireSession(idleToken, false);
        assertInvalidSession(idleToken);
        assertThat(revocationReason(idleToken)).isEqualTo("IDLE_TIMEOUT");

        String absoluteToken = cookieValue(login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andReturn());
        expireSession(absoluteToken, true);
        assertInvalidSession(absoluteToken);
        assertThat(revocationReason(absoluteToken)).isEqualTo("ABSOLUTE_TIMEOUT");
    }

    @Test
    @DisplayName("TC-S2-SESS-008 登录后账号停用使当前会话失效且不暴露状态")
    void disabledAccountInvalidatesExistingSession() throws Exception {
        String username = "session-disabled-" + UUID.randomUUID();
        String password = "test-only-session-disabled";
        Account account = Account.newAccount(
                UUID.randomUUID(),
                new AccountUsername(username),
                passwordHasher.hash(password),
                "会话停用测试账号",
                AccountStatus.ACTIVE);
        accountRepository.insertIfAbsent(account);
        accountRepository.assignRoleIfAbsent(account.id(), RoleCode.CASHIER);
        String rawToken = cookieValue(
                login(username, password, null).andExpect(status().isOk()).andReturn());
        jdbcClient
                .sql("UPDATE accounts SET status = 'DISABLED', updated_at = clock_timestamp() WHERE id = :id")
                .param("id", account.id())
                .update();

        MvcResult result = assertInvalidSession(rawToken);

        assertThat(revocationReason(rawToken)).isEqualTo("ACCOUNT_DISABLED");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(username, "DISABLED", rawToken, tokenIssuer.hash(rawToken));
    }

    @Test
    @DisplayName("TC-S2-SESS-009 至 013 退出撤销当前会话、幂等且不影响并发会话")
    void logoutIsCsrfProtectedIdempotentAndScopedToCurrentSession() throws Exception {
        String firstToken = cookieValue(login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andReturn());
        String secondToken = cookieValue(login("login-cashier", "test-only-cashier-login", null)
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", "invalid-test-token")
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, secondToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-002"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, secondToken)))
                .andExpect(status().isOk());

        MvcResult logout = logout(firstToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0))
                .andReturn();
        assertThat(revocationReason(firstToken)).isEqualTo("LOGOUT");
        assertThat(logout.getResponse().getContentAsString())
                .doesNotContain(firstToken, tokenIssuer.hash(firstToken), "sessionToken", "Cookie");

        assertInvalidSession(firstToken);
        logout(firstToken).andExpect(status().isOk()).andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0));
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, secondToken)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String username, String password, String previousSessionToken) throws Exception {
        return login(username, password, previousSessionToken, "127.0.0.1", null);
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String username, String password, String previousSessionToken, String sourceAddress, String forwardedFor)
            throws Exception {
        var builder = post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(sourceAddress);
                    return request;
                })
                .header("Origin", ORIGIN)
                .header("X-XSRF-TOKEN", csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password)));
        if (forwardedFor != null) {
            builder.header("X-Forwarded-For", forwardedFor);
        }
        if (previousSessionToken != null) {
            builder.cookie(new Cookie(AuthController.SESSION_COOKIE, previousSessionToken));
        }
        return mockMvc.perform(builder).andExpect(header().exists("X-Request-Id"));
    }

    private void assertUnifiedAuthenticationFailure(String username, String password, String sourceAddress)
            throws Exception {
        MvcResult result = login(username, password, null, sourceAddress, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-002"))
                .andExpect(jsonPath("$.message").value("账号或密码错误，或账号不可用"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(username, password, "passwordHash", "sessionToken", "CUP_FLOW_SESSION", "Exception");
    }

    private org.springframework.test.web.servlet.ResultActions logout(String rawSessionToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrfToken())
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, rawSessionToken)))
                .andExpect(header().exists("X-Request-Id"));
    }

    private MvcResult assertInvalidSession(String rawToken) throws Exception {
        return mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie(AuthController.SESSION_COOKIE, rawToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-001"))
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0))
                .andReturn();
    }

    private void expireSession(String rawToken, boolean absolute) {
        String sql = absolute ? """
                  UPDATE auth_sessions
                  SET created_at = clock_timestamp() - INTERVAL '9 hours',
                      last_activity_at = clock_timestamp() - INTERVAL '2 hours',
                      idle_expires_at = clock_timestamp() - INTERVAL '1 hour',
                      absolute_expires_at = clock_timestamp() - INTERVAL '1 hour',
                      updated_at = clock_timestamp()
                  WHERE token_hash = :tokenHash
                  """ : """
                  UPDATE auth_sessions
                  SET created_at = clock_timestamp() - INTERVAL '1 hour',
                      last_activity_at = clock_timestamp() - INTERVAL '31 minutes',
                      idle_expires_at = clock_timestamp() - INTERVAL '1 minute',
                      absolute_expires_at = clock_timestamp() + INTERVAL '7 hours',
                      updated_at = clock_timestamp()
                  WHERE token_hash = :tokenHash
                  """;
        jdbcClient.sql(sql).param("tokenHash", tokenIssuer.hash(rawToken)).update();
    }

    private String revocationReason(String rawToken) {
        return jdbcClient
                .sql("SELECT revocation_reason FROM auth_sessions WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenIssuer.hash(rawToken))
                .query(String.class)
                .single();
    }

    private String csrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.path("data").path("token").asText();
    }

    private String cookieValue(MvcResult result) {
        return result.getResponse().getCookie(AuthController.SESSION_COOKIE).getValue();
    }

    private long sessionCount() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM auth_sessions")
                .query(Long.class)
                .single();
    }
}
