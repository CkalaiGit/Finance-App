package com.cairedine.finance.app.intelligence.infrastructure.web;

import com.cairedine.finance.app.intelligence.domain.port.IAiSummaryService;
import com.cairedine.finance.app.intelligence.infrastructure.web.dto.IngestionPendingResponse;
import com.cairedine.finance.app.intelligence.infrastructure.web.dto.IngestionStatusResponse;
import com.cairedine.finance.app.intelligence.infrastructure.web.dto.InsightResponse;
import com.cairedine.finance.app.shared.exceptions.InvalidTickerException;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Insights", description = "Consultation des synthèses financières générées depuis les rapports SEC")
public class InsightController {

    private static final String INGESTION_PENDING = "INGESTION_PENDING";
    private static final String PENDING_MESSAGE = "Les insights sont en cours de génération.";

    private final IAiSummaryService aiSummaryService;
    private final InsightWebMapper webMapper;

    @GetMapping("/{ticker}")
    @Operation(summary = "Récupérer les insights d'une entreprise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Insights disponibles",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = InsightResponse.class)))),
            @ApiResponse(responseCode = "202", description = "Ingestion des insights en attente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IngestionPendingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ticker invalide", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ticker inconnu", content = @Content)
    })
    public ResponseEntity<?> getInsights(
            @PathVariable
            @Parameter(description = "Symbole boursier de l'entreprise", example = "AAPL")
            String ticker,
            @Parameter(hidden = true) JwtAuthenticationToken auth) {
        String normalizedTicker = normalizeTicker(ticker);
        logAudit(auth, normalizedTicker);

        if (aiSummaryService.isReady(normalizedTicker)) {
            List<InsightResponse> insights = aiSummaryService.getInsights(normalizedTicker).stream()
                    .map(webMapper::toResponse)
                    .toList();
            return ResponseEntity.ok(insights);
        }
        if (!aiSummaryService.isTickerSupported(normalizedTicker)) {
            throw new TickerNotFoundException(normalizedTicker);
        }
        return ResponseEntity.accepted()
                .body(new IngestionPendingResponse(INGESTION_PENDING, PENDING_MESSAGE));
    }

    @GetMapping("/{ticker}/status")
    @Operation(summary = "Récupérer l'état d'ingestion des insights d'une entreprise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "État d'ingestion récupéré",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IngestionStatusResponse.class))),
            @ApiResponse(responseCode = "202", description = "Ingestion des insights en attente", content = @Content),
            @ApiResponse(responseCode = "400", description = "Ticker invalide", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "404", description = "Ticker inconnu", content = @Content)
    })
    public ResponseEntity<IngestionStatusResponse> getStatus(
            @PathVariable
            @Parameter(description = "Symbole boursier de l'entreprise", example = "AAPL")
            String ticker,
            @Parameter(hidden = true) JwtAuthenticationToken auth) {
        String normalizedTicker = normalizeTicker(ticker);
        logAudit(auth, normalizedTicker);
        return ResponseEntity.ok(webMapper.toResponse(aiSummaryService.getStatus(normalizedTicker)));
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new InvalidTickerException("Le ticker ne peut pas être vide");
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private void logAudit(JwtAuthenticationToken auth, String ticker) {
        log.info("Consultation des insights: keycloakId={}, ticker={}",
                auth.getToken().getSubject(), ticker);
    }
}
