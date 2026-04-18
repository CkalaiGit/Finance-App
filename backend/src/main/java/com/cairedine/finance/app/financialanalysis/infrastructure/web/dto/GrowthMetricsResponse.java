package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Métriques mesurant la croissance de l'entreprise")
public record GrowthMetricsResponse(
    @Schema(description = "Croissance du chiffre d'affaires sur 3 ans (%)", example = "15.5")
    BigDecimal revenueGrowth3Y,
    
    @Schema(description = "Croissance de l'EBITDA sur 3 ans (%)", example = "12.0")
    BigDecimal ebitdaGrowth3Y,
    
    @Schema(description = "Croissance du bénéfice par action (EPS) sur 3 ans (%)", example = "20.1")
    BigDecimal epsGrowth3Y,
    
    @Schema(description = "Croissance du flux de trésorerie disponible (Free Cash Flow) (%)", example = "10.5")
    BigDecimal fcfGrowth
) {}