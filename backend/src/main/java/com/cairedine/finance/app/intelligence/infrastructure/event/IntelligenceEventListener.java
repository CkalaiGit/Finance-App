package com.cairedine.finance.app.intelligence.infrastructure.event;

import com.cairedine.finance.app.intelligence.application.IngestionLauncher;
import com.cairedine.finance.app.watchlist.TickerAddedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class IntelligenceEventListener {

    private final IngestionLauncher ingestionLauncher;

    // AFTER_COMMIT keeps document retrieval and synthesis outside the watchlist transaction.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTickerAdded(TickerAddedEvent event) {
        ingestionLauncher.launchIfAllowed(event.ticker());
    }
}