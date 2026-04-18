package com.cairedine.finance.app.financialanalysis.infrastructure.web.controller;

import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.FullMetricsResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper.FinancialAnalysisWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analyse Financière", description = "Endpoints pour récupérer les métriques d'analyse financière des entreprises")
public class FinancialAnalysisController {

    private final IFinancialAnalysisService financialAnalysisService;
    private final FinancialAnalysisWebMapper webMapper;

    @GetMapping("/{ticker}")
    @Operation(summary = "Récupérer les métriques d'une entreprise", description = "Calcule et retourne une liste de métriques financières détaillées (croissance, valeur, qualité) pour un ticker donné.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Métriques récupérées avec succès", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = FullMetricsResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Ticker invalide", content = @Content),
        @ApiResponse(responseCode = "404", description = "Données introuvables pour le ticker fourni", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erreur interne lors de la récupération des données", content = @Content)
    })
    public ResponseEntity<List<FullMetricsResponse>> getMetrics(
            @PathVariable 
            @Parameter(description = "Le symbole boursier de l'entreprise (ex: AAPL, MSFT)", example = "AAPL") 
            String ticker) {
        var domainList = financialAnalysisService.computeMetrics(ticker);
        return ResponseEntity.ok(domainList.stream()
                .map(webMapper::toResponse)
                .toList());
    }
}
