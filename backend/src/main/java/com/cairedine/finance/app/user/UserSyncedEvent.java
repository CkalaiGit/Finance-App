package com.cairedine.finance.app.user;

public record UserSyncedEvent(String keycloakId, String email,
                              String username, boolean isNewUser) {}
