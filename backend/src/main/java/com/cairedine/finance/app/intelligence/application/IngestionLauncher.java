package com.cairedine.finance.app.intelligence.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class IngestionLauncher {

    private final DocumentIngestionService documentIngestionService;
    private final ObjectProvider<IngestionLauncher> launcherProxy;
    private final Clock clock;
    private final Duration retryCooldown;
    private final Duration refreshInterval;
    private final ConcurrentMap<String, Instant> lastLaunchByTicker = new ConcurrentHashMap<>();

    public IngestionLauncher(
            DocumentIngestionService documentIngestionService,
            ObjectProvider<IngestionLauncher> launcherProxy,
            Clock clock,
            @Value("${intelligence.ingestion.retry-cooldown:15m}") Duration retryCooldown,
            @Value("${intelligence.ingestion.refresh-interval:24h}") Duration refreshInterval) {
        this.documentIngestionService = documentIngestionService;
        this.launcherProxy = launcherProxy;
        this.clock = clock;
        this.retryCooldown = retryCooldown;
        this.refreshInterval = refreshInterval;
    }

    @Async("intelligenceTaskExecutor")
    public void launch(String ticker) {
        try {
            documentIngestionService.ingestDocuments(normalizeTicker(ticker));
        } catch (Exception exception) {
            log.error("Échec de l'ingestion des insights pour '{}'.", ticker, exception);
        }
    }

    public boolean launchIfAllowed(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        String normalizedTicker = normalizeTicker(ticker);
        if (tryAcquire(normalizedTicker, retryCooldown)) {
            return false;
        }
        launcherProxy.getObject().launch(normalizedTicker);
        return true;
    }

    public boolean refreshIfStale(String ticker) {
        if (ticker == null || ticker.isBlank() || refreshInterval.isZero() || refreshInterval.isNegative()) {
            return false;
        }
        String normalizedTicker = normalizeTicker(ticker);
        if (tryAcquire(normalizedTicker, refreshInterval)) {
            return false;
        }
        launcherProxy.getObject().launch(normalizedTicker);
        return true;
    }

    /**
     * Enregistre un lancement si le délai minimal depuis le dernier lancement est écoulé.
     * Opération atomique par ticker : un seul appelant obtient true pendant un intervalle donné.
     */
    private boolean tryAcquire(String normalizedTicker, Duration minimumGap) {
        Instant now = clock.instant();
        AtomicBoolean allowed = new AtomicBoolean();
        lastLaunchByTicker.compute(normalizedTicker, (key, lastLaunch) -> {
            if (lastLaunch == null || !now.isBefore(lastLaunch.plus(minimumGap))) {
                allowed.set(true);
                return now;
            }
            return lastLaunch;
        });
        return !allowed.get();
    }

    private String normalizeTicker(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
