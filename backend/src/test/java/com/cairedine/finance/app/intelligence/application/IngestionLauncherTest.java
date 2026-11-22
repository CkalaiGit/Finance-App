package com.cairedine.finance.app.intelligence.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    @Test
    void refreshIfStaleLaunchesFirstTimeAndNormalizesTicker() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        IngestionLauncher launcher = createLauncher(ingestionService,
                new MutableClock(Instant.parse("2025-01-01T00:00:00Z")));

        assertThat(launcher.refreshIfStale(" aapl ")).isTrue();
        verify(ingestionService, times(1)).ingestDocuments("AAPL");
    }

    @Test
    void refreshIfStaleIgnoresBlankOrNullTicker() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        IngestionLauncher launcher = createLauncher(ingestionService,
                new MutableClock(Instant.parse("2025-01-01T00:00:00Z")));

        assertThat(launcher.refreshIfStale(null)).isFalse();
        assertThat(launcher.refreshIfStale("")).isFalse();
        assertThat(launcher.refreshIfStale("   ")).isFalse();
        verifyNoInteractions(ingestionService);
    }

    @Test
    void refreshIfStaleIsBlockedWithinInterval() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        IngestionLauncher launcher = createLauncher(ingestionService, clock);

        assertThat(launcher.refreshIfStale("AAPL")).isTrue();
        clock.advance(Duration.ofHours(23).plusMinutes(59));
        assertThat(launcher.refreshIfStale("AAPL")).isFalse();
        verify(ingestionService, times(1)).ingestDocuments("AAPL");
    }

    @Test
    void refreshIfStaleAllowsAfterInterval() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        IngestionLauncher launcher = createLauncher(ingestionService, clock);

        assertThat(launcher.refreshIfStale("AAPL")).isTrue();
        clock.advance(Duration.ofHours(24));
        assertThat(launcher.refreshIfStale("AAPL")).isTrue();
        verify(ingestionService, times(2)).ingestDocuments("AAPL");
    }

    @Test
    void launchIfAllowedAfterRefreshRespectsOnlyItsOwnCooldown() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        IngestionLauncher launcher = createLauncher(ingestionService, clock);

        assertThat(launcher.refreshIfStale("AAPL")).isTrue();
        clock.advance(Duration.ofMinutes(5));
        assertThat(launcher.launchIfAllowed("AAPL")).isFalse();
        clock.advance(Duration.ofMinutes(10));
        assertThat(launcher.launchIfAllowed("AAPL")).isTrue();
    }

    @Test
    void launchIfAllowedCountsAsRecentCheckForRefresh() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        IngestionLauncher launcher = createLauncher(ingestionService, clock);

        assertThat(launcher.launchIfAllowed("AAPL")).isTrue();
        clock.advance(Duration.ofHours(1));
        assertThat(launcher.refreshIfStale("AAPL")).isFalse();
        clock.advance(Duration.ofHours(23));
        assertThat(launcher.refreshIfStale("AAPL")).isTrue();
        verify(ingestionService, times(2)).ingestDocuments("AAPL");
    }

    @Test
    void disabledRefreshNeverLaunches() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        IngestionLauncher zero = createLauncher(ingestionService, clock, Duration.ZERO);
        IngestionLauncher negative = createLauncher(ingestionService, clock, Duration.ofHours(-1));

        assertThat(zero.refreshIfStale("AAPL")).isFalse();
        assertThat(negative.refreshIfStale("AAPL")).isFalse();
        verifyNoInteractions(ingestionService);

        assertThat(zero.launchIfAllowed("AAPL")).isTrue();
        verify(ingestionService, times(1)).ingestDocuments("AAPL");
    }

    @Test
    void concurrentRefreshesLaunchExactlyOnce() throws Exception {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        IngestionLauncher launcher = createLauncher(ingestionService,
                new MutableClock(Instant.parse("2025-01-01T00:00:00Z")));
        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return launcher.refreshIfStale("AAPL");
                }));
            }
            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
        verify(ingestionService, times(1)).ingestDocuments("AAPL");
    }

    @Test
    void refreshExceptionsAreLoggedNotPropagated() {
        DocumentIngestionService ingestionService = mock(DocumentIngestionService.class);
        doThrow(new IllegalStateException("ingestion failed"))
                .when(ingestionService).ingestDocuments("AAPL");
        IngestionLauncher launcher = createLauncher(ingestionService,
                new MutableClock(Instant.parse("2025-01-01T00:00:00Z")));

        AtomicReference<Boolean> result = new AtomicReference<>();
        assertThatCode(() -> result.set(launcher.refreshIfStale("AAPL"))).doesNotThrowAnyException();
        assertThat(result.get()).isTrue();
        verify(ingestionService).ingestDocuments("AAPL");
    }

    private IngestionLauncher createLauncher(DocumentIngestionService ingestionService, Clock clock) {
        return createLauncher(ingestionService, clock, Duration.ofHours(24));
    }

    private IngestionLauncher createLauncher(DocumentIngestionService ingestionService, Clock clock,
                                             Duration refreshInterval) {
        ObjectProvider<IngestionLauncher> launcherProxy = mock(ObjectProvider.class);
        IngestionLauncher launcher = new IngestionLauncher(
                ingestionService, launcherProxy, clock, Duration.ofMinutes(15), refreshInterval);
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
