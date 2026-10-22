package com.cairedine.finance.app.user.domain.model;

import java.util.Set;

/**
 * Informations d'identité extraites d'un token d'authentification.
 * Value Object pur du domaine, découplé de tout framework de sécurité (Spring Security, Jwt).
 */
public record UserClaims(
        String keycloakId,
        String email,
        String username,
        Set<String> roles
) {
    public UserClaims {
        roles = (roles != null) ? Set.copyOf(roles) : Set.of();
    }
}
