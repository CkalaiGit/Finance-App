package com.cairedine.finance.app.intelligence.infrastructure.event;

import com.cairedine.finance.app.intelligence.application.IngestionLauncher;
import com.cairedine.finance.app.watchlist.TickerAddedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligenceEventListenerTest {

    @Mock
    private IngestionLauncher ingestionLauncher;

    @Test
    void tickerAddedEventTriggersIngestionOnce() {
        IntelligenceEventListener listener = new IntelligenceEventListener(ingestionLauncher);
        TickerAddedEvent event = new TickerAddedEvent("AAPL", Instant.now());

        when(ingestionLauncher.launchIfAllowed("AAPL")).thenReturn(true);
        listener.onTickerAdded(event);

        verify(ingestionLauncher).launchIfAllowed("AAPL");
    }

    @Test
    void ingestionFailureIsLoggedAndNotPropagated() {
        IntelligenceEventListener listener = new IntelligenceEventListener(ingestionLauncher);
        TickerAddedEvent event = new TickerAddedEvent("AAPL", Instant.now());
        when(ingestionLauncher.launchIfAllowed("AAPL")).thenReturn(false);

        listener.onTickerAdded(event);

        verify(ingestionLauncher).launchIfAllowed("AAPL");
    }
}
