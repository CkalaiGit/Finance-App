package com.cairedine.finance.app.user.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cairedine.finance.app.user.UserContext;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateur", description = "Endpoints de gestion du profil utilisateur et de l'authentification")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Récupère le profil utilisateur connecté", description = "Retourne le contexte de l'utilisateur actuellement authentifié à partir de son token JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil récupéré avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserContext.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié ou token invalide", content = @Content)
    })
    public ResponseEntity<@NonNull UserContext> me(
            @Parameter(hidden = true) JwtAuthenticationToken auth) {
        UserContext ctx = (UserContext) auth.getDetails();
        return ResponseEntity.ok(ctx);
    }
}
