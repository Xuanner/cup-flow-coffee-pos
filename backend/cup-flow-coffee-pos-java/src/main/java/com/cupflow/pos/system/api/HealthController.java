package com.cupflow.pos.system.api;

import com.cupflow.pos.shared.api.ApiResponse;
import com.cupflow.pos.shared.logging.TraceContext;
import com.cupflow.pos.system.application.HealthService;
import com.cupflow.pos.system.application.SystemHealth;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<SystemHealth>> health() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(healthService.check(), TraceContext.currentTraceId()));
    }
}
