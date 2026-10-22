package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Métriques évaluant la qualité et la solidité de l'entreprise")
public record QualityMetricsResponse(
    @Schema(description = "Retour sur Capital Investi (ROIC en %)", example = "18.5")
    BigDecimal roic,
    
    @Schema(description = "Marge opérationnelle (%)", example = "22.0")
    BigDecimal operatingMargin,
    
    @Schema(description = "Ratio de Dette Nette sur EBITDA", example = "1.5")
    BigDecimal netDebtToEbitda,
    
    @Schema(description = "Marge de flux de trésorerie disponible (FCF Margin en %)", example = "15.0")
    BigDecimal freeCashFlowMargin,
    
    @Schema(description = "Ratio des Frais Généraux et Administratifs sur le Chiffre d'Affaires (SG&A/Revenue en %)", example = "8.5")
    BigDecimal sgaToRevenue
) {}
