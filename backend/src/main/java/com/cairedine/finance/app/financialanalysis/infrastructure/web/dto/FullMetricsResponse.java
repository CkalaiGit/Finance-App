package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Réponse contenant l'ensemble complet des métriques financières")
public record FullMetricsResponse(
    @Schema(description = "Métriques de croissance")
    GrowthMetricsResponse growth,
    
    @Schema(description = "Métriques de valorisation")
    ValueMetricsResponse value,
    
    @Schema(description = "Métriques de qualité")
    QualityMetricsResponse quality,
    
    @Schema(description = "Date de fin de l'année fiscale concernée", example = "2023-12-31")
    String fiscalYearEndDate,
    
    @Schema(description = "Date à laquelle les données de marché ont été récupérées", example = "2024-04-18T12:00:00Z")
    Instant marketDataAsOf
) {}
