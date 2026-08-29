package com.cupflow.pos.auth.application;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserContext {

    private final ThreadLocal<CurrentUser> current = new ThreadLocal<>();

    public void set(CurrentUser user) {
        current.set(user);
    }

    public Optional<CurrentUser> get() {
        return Optional.ofNullable(current.get());
    }

    public CurrentUser requireCurrentUser() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user is bound to this request"));
    }

    public void clear() {
        current.remove();
    }
}
