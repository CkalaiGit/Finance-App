package com.cairedine.finance.app.intelligence.infrastructure.event;

import com.cairedine.finance.app.intelligence.application.DocumentIngestionService;
import com.cairedine.finance.app.watchlist.TickerAddedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IntelligenceEventListenerTest {

    @Mock
    private DocumentIngestionService documentIngestionService;

    @Test
    void tickerAddedEventTriggersIngestionOnce() {
        IntelligenceEventListener listener = new IntelligenceEventListener(documentIngestionService);
        TickerAddedEvent event = new TickerAddedEvent("AAPL", Instant.now());

        listener.onTickerAdded(event);

        verify(documentIngestionService).ingestDocuments("AAPL");
    }

    @Test
    void ingestionFailureIsLoggedAndNotPropagated() {
        IntelligenceEventListener listener = new IntelligenceEventListener(documentIngestionService);
        TickerAddedEvent event = new TickerAddedEvent("AAPL", Instant.now());
        doThrow(new IllegalStateException("ingestion failed"))
                .when(documentIngestionService).ingestDocuments("AAPL");

        assertThatCode(() -> listener.onTickerAdded(event)).doesNotThrowAnyException();

        verify(documentIngestionService).ingestDocuments("AAPL");
    }
}
