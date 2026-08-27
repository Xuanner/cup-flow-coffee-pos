package com.cupflow.pos.auth.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @TrimmedSize(min = 1, max = 64, message = "账号长度必须为 1 到 64 个字符")
        String username,

        @NotNull(message = "密码不能为空") @Size(min = 1, max = 128, message = "密码长度必须为 1 到 128 个字符") String password) {}
