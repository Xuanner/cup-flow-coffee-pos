package com.cupflow.pos.system.application;

import com.cupflow.pos.system.infrastructure.DatabaseProbeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthService {

    private final DatabaseProbeMapper databaseProbeMapper;

    public HealthService(DatabaseProbeMapper databaseProbeMapper) {
        this.databaseProbeMapper = databaseProbeMapper;
    }

    @Transactional(readOnly = true)
    public SystemHealth check() {
        boolean databaseUp = databaseProbeMapper.probe() == 1;
        return new SystemHealth("UP", databaseUp ? "UP" : "DOWN");
    }
}
