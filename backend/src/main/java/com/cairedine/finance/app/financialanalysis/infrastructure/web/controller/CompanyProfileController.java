package com.cairedine.finance.app.financialanalysis.infrastructure.web.controller;

import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.CompanyProfileResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper.CompanyProfileWebMapper;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.CompanyProfileRecord;
import com.cairedine.finance.app.webclient.IMarketDataPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST Controller pour exposer les données de profil d'entreprise.
 * Architecture hexagonale :
 * - Opère comme la couche de présentation (Web Layer)
 * - Utilise IMarketDataPort (port d'entrée) pour récupérer les données
 * - Mappe les données du domaine vers les DTOs de réponse
 * Responsabilité : Fournir un endpoint HTTP facile d'accès pour les données statiques
 * d'une entreprise (profil, industrie, prix, description, etc.)
 * Avantage sur l'enrichissement de /api/v1/analysis :
 * - Les clients peuvent récupérer UNIQUEMENT le profil s'ils le souhaitent
 * - Pas de surcharge de données inutiles
 * - Endpoint réutilisable dans d'autres contextes (recherche, autocomplete, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/company-profile")
@RequiredArgsConstructor
@Tag(name = "Profil de l'Entreprise", description = "Endpoints pour récupérer les informations de profil d'une entreprise")
public class CompanyProfileController {

    private final IMarketDataPort marketDataPort;
    private final CompanyProfileWebMapper profileMapper;

    @GetMapping("/{ticker}")
    @Operation(
        summary = "Récupérer le profil d'une entreprise",
        description = "Retourne les informations statiques d'une entreprise : industrie, description, prix, capitalisation, etc."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profil récupéré avec succès",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CompanyProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Ticker invalide"),
        @ApiResponse(responseCode = "404", description = "Profil non trouvé pour le ticker fourni"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération du profil")
    })
    public ResponseEntity<CompanyProfileResponse> getCompanyProfile(
            @PathVariable
            @Parameter(description = "Le symbole boursier de l'entreprise (ex: AAPL, MSFT, GOOGL)", example = "AAPL")
            String ticker) {

        String normalizedTicker = ticker.toUpperCase();
        log.info("Récupération du profil pour le ticker: {}", normalizedTicker);

        Optional<CompanyProfileRecord> profileOptional = marketDataPort.fetchCompanyProfile(normalizedTicker);

        if (profileOptional.isEmpty()) {
            log.warn("Profil non trouvé pour le ticker: {}", normalizedTicker);
            throw new TickerNotFoundException(normalizedTicker);
        }

        var response = profileMapper.toResponse(profileOptional.get());
        return ResponseEntity.ok(response);
    }
}

