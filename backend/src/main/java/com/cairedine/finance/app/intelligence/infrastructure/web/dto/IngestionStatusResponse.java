package com.cairedine.finance.app.intelligence.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "État des insights disponibles pour une entreprise")
public record IngestionStatusResponse(
        @Schema(description = "Symbole boursier de l'entreprise", example = "AAPL")
        String ticker,
        @Schema(description = "Indique si au moins un insight non expiré est disponible", example = "true")
        boolean ready,
        @Schema(description = "Nombre d'insights non expirés disponibles", example = "3")
        int count,
        @Schema(description = "Date de génération la plus récente, ou null si aucun insight n'est disponible")
        Instant lastGenerated
) {
}
