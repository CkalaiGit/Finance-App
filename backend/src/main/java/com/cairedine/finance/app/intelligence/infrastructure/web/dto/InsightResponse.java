package com.cairedine.finance.app.intelligence.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Synthèse financière d'une entreprise")
public record InsightResponse(
        @Schema(description = "Symbole boursier de l'entreprise", example = "AAPL")
        String ticker,
        @Schema(description = "Type de rapport SEC", example = "10-K")
        String formType,
        @Schema(description = "Période fiscale du rapport", example = "FY 2025")
        String period,
        @Schema(description = "Numéro d'accession SEC du rapport")
        String accessionNumber,
        @Schema(description = "Synthèse de la performance financière")
        String synthesePerformance,
        @Schema(description = "Analyse des marges et de la dette")
        String analyseMargesEtDette,
        @Schema(description = "Principaux risques étayés par le rapport")
        List<String> risquesPrincipaux,
        @Schema(description = "Guidance communiquée par le management")
        String guidanceManagement,
        @Schema(description = "Informations sur la chaîne d'approvisionnement")
        String chaineApprovisionnement,
        @Schema(description = "Faits marquants du rapport")
        List<String> faitsMarquants,
        @Schema(description = "Date de génération de la synthèse", example = "2025-03-01T12:00:00Z")
        Instant generatedAt
) {
}
