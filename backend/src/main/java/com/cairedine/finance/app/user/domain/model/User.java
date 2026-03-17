package com.cairedine.finance.app.user.domain.model;

import java.util.Set;

public record User(
        String keycloakId,
        String email,
        String username,
        Set<String> roles
) {
    public User updateFrom(String email, String username, Set<String> roles) {
        return new User(this.keycloakId, email, username, roles);
    }
}
