package com.cupflow.pos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cupflow.pos.TestcontainersConfiguration;
import com.cupflow.pos.auth.api.AuthController;
import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.logging.TraceContext;
import com.cupflow.pos.shared.security.EndpointRole;
import com.cupflow.pos.shared.security.RequiresRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, AuthorizationIntegrationTest.ProbeConfiguration.class})
@SpringBootTest(
        properties = {
            "auth.bootstrap.enabled=true",
            "auth.bootstrap.cashier.username=authz-cashier",
            "auth.bootstrap.cashier.password=test-only-authz-cashier",
            "auth.bootstrap.cashier.display-name=权限收银员",
            "auth.bootstrap.admin.username=authz-admin",
            "auth.bootstrap.admin.password=test-only-authz-admin",
            "auth.bootstrap.admin.display-name=权限管理员",
            "auth.security.allowed-origins=http://localhost:5173",
            "auth.security.cookie-secure=false"
        })
@AutoConfigureMockMvc
class AuthorizationIntegrationTest {

    private static final String ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProbeService probeService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void resetProbe() {
        probeService.reset();
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-001 未登录访问受保护和未声明接口统一返回 401 且不执行业务")
    void anonymousRequestsAreRejectedBeforeBusinessExecution() throws Exception {
        for (String path : new String[] {
            "/api/v1/authz-probe/cashier", "/api/v1/authz-probe/admin", "/api/v1/authz-probe/unclassified"
        }) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-401-001"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
        assertThat(probeService.calls()).isZero();
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-002/004/009 CASHIER 可访问收银接口但管理员接口返回 403")
    void cashierHasOnlyCashierAccess() throws Exception {
        String token = login("authz-cashier", "test-only-authz-cashier");

        mockMvc.perform(get("/api/v1/authz-probe/cashier").cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isOk());
        MvcResult forbidden = mockMvc.perform(
                        get("/api/v1/authz-probe/admin").cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-001"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();

        assertThat(probeService.calls()).isEqualTo(1);
        assertThat(forbidden.getResponse().getContentAsString())
                .doesNotContain(token, "CASHIER", "ADMIN", "requiredRole", "Cookie");
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-003/005 ADMIN 继承收银接口并访问管理员接口")
    void adminCanAccessBothRoleLevels() throws Exception {
        String token = login("authz-admin", "test-only-authz-admin");

        mockMvc.perform(get("/api/v1/authz-probe/cashier").cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/authz-probe/admin").cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isOk());

        assertThat(probeService.calls()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-006 未声明业务接口对已登录角色默认返回 403")
    void undeclaredEndpointDefaultsToDeny() throws Exception {
        String cashierToken = login("authz-cashier", "test-only-authz-cashier");
        String adminToken = login("authz-admin", "test-only-authz-admin");

        for (String token : new String[] {cashierToken, adminToken}) {
            mockMvc.perform(get("/api/v1/authz-probe/unclassified")
                            .cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH-403-001"));
        }
        assertThat(probeService.calls()).isZero();
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-007 客户端伪造角色字段不能提升权限")
    void forgedClientRoleCannotElevateAccess() throws Exception {
        String token = login("authz-cashier", "test-only-authz-cashier");

        mockMvc.perform(get("/api/v1/authz-probe/admin?role=ADMIN")
                        .header("X-Role", "ADMIN")
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-403-001"));
        assertThat(probeService.calls()).isZero();
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-008 无效会话访问管理员接口返回 401 而非 403")
    void invalidSessionIsUnauthenticatedRatherThanForbidden() throws Exception {
        String forgedToken = "A".repeat(43);

        mockMvc.perform(get("/api/v1/authz-probe/admin").cookie(new Cookie(AuthController.SESSION_COOKIE, forgedToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401-001"))
                .andExpect(cookie().maxAge(AuthController.SESSION_COOKIE, 0));
        assertThat(probeService.calls()).isZero();
    }

    @Test
    @DisplayName("TC-S2-AUTHZ-010 CSRF、登录和健康检查保持公开")
    void approvedPublicEndpointsRemainAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        login("authz-cashier", "test-only-authz-cashier");
    }

    private String login(String username, String password) throws Exception {
        String csrf = csrfToken();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(AuthController.SESSION_COOKIE).getValue();
    }

    private String csrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.path("data").path("token").asText();
    }

    record Credentials(String username, String password) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        ProbeService probeService() {
            return new ProbeService();
        }

        @Bean
        ProbeController probeController(ProbeService probeService) {
            return new ProbeController(probeService);
        }
    }

    static class ProbeService {

        private final AtomicInteger calls = new AtomicInteger();

        String execute() {
            calls.incrementAndGet();
            return "allowed";
        }

        int calls() {
            return calls.get();
        }

        void reset() {
            calls.set(0);
        }
    }

    @RestController
    @RequestMapping("/api/v1/authz-probe")
    static class ProbeController {

        private final ProbeService probeService;

        ProbeController(ProbeService probeService) {
            this.probeService = probeService;
        }

        @GetMapping("/cashier")
        @RequiresRole(EndpointRole.CASHIER)
        ApiResponse<String> cashier() {
            return ApiResponse.success(probeService.execute(), TraceContext.currentTraceId());
        }

        @GetMapping("/admin")
        @RequiresRole(EndpointRole.ADMIN)
        ApiResponse<String> admin() {
            return ApiResponse.success(probeService.execute(), TraceContext.currentTraceId());
        }

        @GetMapping("/unclassified")
        ApiResponse<String> unclassified() {
            return ApiResponse.success(probeService.execute(), TraceContext.currentTraceId());
        }
    }
}
