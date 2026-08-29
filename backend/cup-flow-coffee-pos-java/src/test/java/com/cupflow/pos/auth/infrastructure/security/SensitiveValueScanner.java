package com.cupflow.pos.auth.infrastructure.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
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

    static Map<String, String> readTextArtifacts(Path root) throws IOException {
        Map<String, String> artifacts = new LinkedHashMap<>();
        if (!Files.exists(root)) {
            return artifacts;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                artifacts.put(root.relativize(path).toString(), Files.readString(path));
            }
        }
        return artifacts;
    }
}
