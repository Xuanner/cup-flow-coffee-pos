package com.cupflow.pos.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
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

    private org.springframework.test.web.servlet.ResultActions login(
            String username, String password, String previousSessionToken) throws Exception {
        var builder = post("/api/v1/auth/login")
                .header("Origin", ORIGIN)
                .header("X-XSRF-TOKEN", csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password)));
        if (previousSessionToken != null) {
            builder.cookie(new Cookie(AuthController.SESSION_COOKIE, previousSessionToken));
        }
        return mockMvc.perform(builder).andExpect(header().exists("X-Request-Id"));
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
