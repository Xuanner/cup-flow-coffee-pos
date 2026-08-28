package com.cupflow.pos.auth.application;

public sealed interface CurrentSessionResult {

    record Authenticated(CurrentUser currentUser) implements CurrentSessionResult {}

    record Invalid() implements CurrentSessionResult {}
}
