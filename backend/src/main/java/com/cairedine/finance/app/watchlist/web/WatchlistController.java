package com.cairedine.finance.app.watchlist.web;

import com.cairedine.finance.app.watchlist.domain.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Contrôleur REST pour la gestion de la watchlist.
 * La sécurité est assurée par l'extraction du keycloakId du JWT.
 * Aucun ID utilisateur n'est accepté en paramètre.
 */
@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist", description = "Endpoints de gestion de la watchlist utilisateur")
public class WatchlistController {

    private final WatchlistService watchlistService;

    /**
     * Ajoute un ticker à la watchlist de l'utilisateur connecté
     */
    @PostMapping("/{ticker}")
    @Operation(summary = "Ajouter un ticker à la watchlist")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ticker ajouté avec succès"),
        @ApiResponse(responseCode = "400", description = "Ticker invalide ou déjà existant"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<WatchlistResponse> addTicker(
            @PathVariable
            @Parameter(description = "Le code ticker du titre (ex: AAPL, MSFT)", example = "AAPL")
            String ticker,
            @Parameter(hidden = true) JwtAuthenticationToken auth) {

        String keycloakId = extractKeycloakId(auth);
        watchlistService.addTickerToWatchlist(keycloakId, ticker);
        Set<String> updatedWatchlist = watchlistService.getUserWatchlist(keycloakId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new WatchlistResponse(keycloakId, updatedWatchlist));
    }

    /**
     * Supprime un ticker de la watchlist de l'utilisateur connecté
     */
    @DeleteMapping("/{ticker}")
    @Operation(summary = "Supprimer un ticker de la watchlist")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticker supprimé avec succès"),
        @ApiResponse(responseCode = "400", description = "Ticker n'existe pas dans la watchlist"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<WatchlistResponse> removeTicker(
            @PathVariable
            @Parameter(description = "Le code ticker à supprimer", example = "AAPL")
            String ticker,
            @Parameter(hidden = true) JwtAuthenticationToken auth) {

        String keycloakId = extractKeycloakId(auth);
        watchlistService.removeTickerFromWatchlist(keycloakId, ticker);
        Set<String> updatedWatchlist = watchlistService.getUserWatchlist(keycloakId);
        return ResponseEntity.ok(new WatchlistResponse(keycloakId, updatedWatchlist));
    }

    /**
     * Récupère la watchlist complète de l'utilisateur connecté
     */
    @GetMapping
    @Operation(summary = "Récupérer la watchlist de l'utilisateur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watchlist récupérée avec succès"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<WatchlistResponse> getWatchlist(@Parameter(hidden = true) JwtAuthenticationToken auth) {
        String keycloakId = extractKeycloakId(auth);
        Set<String> watchlist = watchlistService.getUserWatchlist(keycloakId);
        return ResponseEntity.ok(new WatchlistResponse(keycloakId, watchlist));
    }

    /**
     * Vérifie si un ticker est dans la watchlist
     */
    @GetMapping("/{ticker}/exists")
    @Operation(summary = "Vérifier si un ticker est dans la watchlist")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vérification effectuée"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    public ResponseEntity<TickerExistsResponse> checkTickerExists(
            @PathVariable
            @Parameter(description = "Le code ticker à vérifier", example = "AAPL")
            String ticker,
            @Parameter(hidden = true) JwtAuthenticationToken auth) {

        String keycloakId = extractKeycloakId(auth);
        boolean exists = watchlistService.isTickerInWatchlist(keycloakId, ticker);
        return ResponseEntity.ok(new TickerExistsResponse(ticker, exists));
    }

    /**
     * Extrait le keycloakId du JWT
     */
    private String extractKeycloakId(JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        String keycloakId = jwt.getSubject();

        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("keycloakId non trouvé dans le JWT");
        }

        return keycloakId;
    }

    /**
     * DTO de réponse pour la watchlist
     */
    @Schema(description = "DTO de réponse contenant la watchlist de l'utilisateur")
    public record WatchlistResponse(
            @Schema(description = "Identifiant de l'utilisateur", example = "user-123")
            String keycloakId, 
            @Schema(description = "Ensemble de tickers surveillés", example = "[\"AAPL\", \"MSFT\"]")
            Set<String> tickers) {
    }

    /**
     * DTO de réponse pour la vérification d'existence
     */
    @Schema(description = "DTO de réponse pour la vérification de présence d'un ticker")
    public record TickerExistsResponse(
            @Schema(description = "Le ticker vérifié", example = "AAPL")
            String ticker, 
            @Schema(description = "Indique si le ticker est présent dans la watchlist", example = "true")
            boolean exists) {
    }
}

