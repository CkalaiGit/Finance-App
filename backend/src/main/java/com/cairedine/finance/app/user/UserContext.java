package com.cairedine.finance.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

/**
 * Contrat public du module user.
 * Consommé par tous les autres modules via SecurityContextHolder.
 * Immuable par nature (record Java).
 */
@Schema(description = "Contexte de l'utilisateur actuellement authentifié")
public record UserContext(
        @Schema(description = "Identifiant unique de l'utilisateur (ex: Keycloak ID)", example = "user-1234-5678")
        String id,
        
        @Schema(description = "Adresse email de l'utilisateur", example = "jean.dupont@example.com")
        String email,
        
        @Schema(description = "Nom d'utilisateur", example = "jdupont")
        String username,
        
        @Schema(description = "Rôles assignés à l'utilisateur", example = "[\"ROLE_USER\", \"ROLE_PREMIUM\"]")
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}