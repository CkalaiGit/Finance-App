package com.cairedine.finance.app.intelligence.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class IngestionLauncherTest {

    @Test
    void enforcesPerTickerCooldownAndAllowsRetryAfterItExpires() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        IngestionLauncher launcher = createLauncher(ingestionService, clock);

        assertThat(launcher.launchIfAllowed("aapl")).isTrue();
        assertThat(launcher.launchIfAllowed("AAPL")).isFalse();
        verify(ingestionService, times(1)).ingestDocuments("AAPL");

        clock.advance(Duration.ofMinutes(15));
        assertThat(launcher.launchIfAllowed("AAPL")).isTrue();
        verify(ingestionService, times(2)).ingestDocuments("AAPL");
    }

    @Test
    void logsIngestionExceptionsWithoutPropagatingThem() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        doThrow(new IllegalStateException("ingestion failed"))
                .when(ingestionService).ingestDocuments("AAPL");
        IngestionLauncher launcher = createLauncher(ingestionService, Clock.systemUTC());

        assertThatCode(() -> launcher.launchIfAllowed("AAPL")).doesNotThrowAnyException();
        verify(ingestionService).ingestDocuments("AAPL");
    }

    private IngestionLauncher createLauncher(DocumentIngestionService ingestionService, Clock clock) {
        ObjectProvider<IngestionLauncher> launcherProxy = mock(ObjectProvider.class);
        IngestionLauncher launcher = new IngestionLauncher(
                ingestionService, launcherProxy, clock, Duration.ofMinutes(15));
        when(launcherProxy.getObject()).thenReturn(launcher);
        return launcher;
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }

        private void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }
    }
}
