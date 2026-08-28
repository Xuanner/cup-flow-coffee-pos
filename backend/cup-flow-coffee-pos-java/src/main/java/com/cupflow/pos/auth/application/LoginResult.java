package com.cupflow.pos.auth.application;

public sealed interface LoginResult {

    record Success(CurrentUser currentUser, String sessionToken) implements LoginResult {
        @Override
        public String toString() {
            return "Success[currentUser=" + currentUser + ", sessionToken=[REDACTED]]";
        }
    }

    record Failure() implements LoginResult {}

    record RateLimited(long retryAfterSeconds) implements LoginResult {
        public RateLimited {
            if (retryAfterSeconds < 1) {
                throw new IllegalArgumentException("retryAfterSeconds must be positive");
            }
        }
    }
}
