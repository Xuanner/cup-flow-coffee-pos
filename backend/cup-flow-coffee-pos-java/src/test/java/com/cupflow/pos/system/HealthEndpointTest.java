package com.cupflow.pos.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cupflow.pos.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TC-S1-HEALTH-001 返回应用与数据库健康状态")
    void returnsApplicationAndDatabaseHealthInApiEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "test-trace-12345"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-trace-12345"))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.application").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.traceId").value("test-trace-12345"));
    }

    @Test
    @DisplayName("TC-S1-HEALTH-002 替换不安全的追踪 ID")
    void replacesUnsafeTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "contains spaces"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("TC-S1-HEALTH-003 未知 API 返回统一 404 响应")
    void returnsUnifiedResponseForUnknownApiResource() throws Exception {
        mockMvc.perform(get("/api/v1/not-found").header("X-Request-Id", "missing-route-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON-404-001"))
                .andExpect(jsonPath("$.message").value("请求的资源不存在"))
                .andExpect(jsonPath("$.traceId").value("missing-route-001"));
    }
}
