package com.cairedine.finance.app.financialanalysis.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Métriques d'évaluation de la valorisation boursière de l'entreprise")
public record ValueMetricsResponse(
    @Schema(description = "Ratio Valeur d'Entreprise sur EBIT (EV/EBIT)", example = "15.2")
    BigDecimal evToEbit,
    
    @Schema(description = "Ratio Prix sur Bénéfice sur 12 mois glissants (P/E TTM)", example = "25.4")
    BigDecimal peRatioTTM,
    
    @Schema(description = "Ratio P/E ajusté de la croissance (PEG Ratio) prévisionnel", example = "1.2")
    BigDecimal pegRatioForward,
    
    @Schema(description = "Ratio Valeur d'Entreprise sur Chiffre d'Affaires (EV/Sales)", example = "4.5")
    BigDecimal evToSales
) {}
