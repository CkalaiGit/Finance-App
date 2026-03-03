package com.cairedine.finance.app.user;

import java.util.Set;

/**
 * Contrat public du module user.
 * Consommé par tous les autres modules via SecurityContextHolder.
 * Immuable par nature (record Java).
 */
public record UserContext(
        String id,
        String email,
        String username,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}