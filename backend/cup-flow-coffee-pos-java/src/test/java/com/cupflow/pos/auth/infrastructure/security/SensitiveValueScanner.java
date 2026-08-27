package com.cupflow.pos.auth.infrastructure.security;

import java.util.Collection;
import java.util.Map;

final class SensitiveValueScanner {

    private SensitiveValueScanner() {}

    static void assertAbsent(Map<String, String> artifacts, Collection<String> sensitiveValues) {
        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            for (String sensitiveValue : sensitiveValues) {
                if (sensitiveValue != null
                        && !sensitiveValue.isEmpty()
                        && artifact.getValue().contains(sensitiveValue)) {
                    throw new AssertionError("Sensitive value detected in artifact: " + artifact.getKey());
                }
            }
        }
    }
}
