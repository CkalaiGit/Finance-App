package com.cairedine.finance.app.intelligence.infrastructure.event;

import com.cairedine.finance.app.intelligence.application.DocumentIngestionService;
import com.cairedine.finance.app.watchlist.TickerAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntelligenceEventListener {

    private final DocumentIngestionService documentIngestionService;

    // AFTER_COMMIT et @Async maintiennent les appels réseau et Gemini hors de la transaction watchlist.
    @Async("intelligenceTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTickerAdded(TickerAddedEvent event) {
        try {
            documentIngestionService.ingestDocuments(event.ticker());
        } catch (Exception exception) {
            log.error("Échec de l'ingestion des insights pour '{}'.", event.ticker(), exception);
        }
    }
}