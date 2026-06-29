package com.cairedine.finance.app.user;

// TODO: make externalizable with Spring Modulith later
// @org.springframework.modulith.events.Externalized
public record UserSyncedEvent(String keycloakId, String email, String username, boolean isNewUser) {
}
