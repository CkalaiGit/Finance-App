package com.cairedine.finance.app.intelligence.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse indiquant que l'ingestion des insights est en attente")
public record IngestionPendingResponse(
        @Schema(description = "Code d'état de l'ingestion", example = "INGESTION_PENDING")
        String status,
        @Schema(description = "Message destiné au client", example = "Les insights sont en cours de génération.")
        String message
) {
}
