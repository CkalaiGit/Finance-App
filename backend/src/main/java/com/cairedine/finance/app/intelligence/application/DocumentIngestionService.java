package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import com.cairedine.finance.app.intelligence.domain.port.IIngestionTracker;
import com.cairedine.finance.app.intelligence.domain.port.ISectionExtractor;
import com.cairedine.finance.app.intelligence.domain.port.ITextCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Service
@Slf4j
public class DocumentIngestionService implements IIngestionTracker {

    private final IDocumentSourcePort documentSource;
    private final IFinancialAnalysisService financialAnalysisService;
    private final ICompanyInsightRepository insightRepository;
    private final ITextCleaner textCleaner;
    private final ISectionExtractor sectionExtractor;
    private final AiSummaryServiceImpl aiSummaryService;
    private final Set<String> ingestingTickers = ConcurrentHashMap.newKeySet();

    public DocumentIngestionService(
            IDocumentSourcePort documentSource,
            IFinancialAnalysisService financialAnalysisService,
            ICompanyInsightRepository insightRepository,
            ITextCleaner textCleaner,
            ISectionExtractor sectionExtractor,
            AiSummaryServiceImpl aiSummaryService) {
        this.documentSource = documentSource;
        this.financialAnalysisService = financialAnalysisService;
        this.insightRepository = insightRepository;
        this.textCleaner = textCleaner;
        this.sectionExtractor = sectionExtractor;
        this.aiSummaryService = aiSummaryService;
    }

    public void ingestDocuments(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            log.warn("Ingestion SEC ignorée: ticker vide.");
            return;
        }

        String normalizedTicker = ticker.trim().toUpperCase(Locale.ROOT);
        if (!ingestingTickers.add(normalizedTicker)) {
            log.info("Ingestion SEC déjà en cours pour '{}'; nouvel appel ignoré.", normalizedTicker);
            return;
        }

        try {
            ingestAvailableDocuments(normalizedTicker);
        } finally {
            ingestingTickers.remove(normalizedTicker);
        }
    }

    @Override
    public boolean isIngesting(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        return ingestingTickers.contains(ticker.trim().toUpperCase(Locale.ROOT));
    }

    private void ingestAvailableDocuments(String normalizedTicker) {
        if (!documentSource.isAvailable(normalizedTicker)) {
            log.info("Ingestion SEC ignorée pour le ticker inconnu '{}'.", normalizedTicker);
            return;
        }

        List<FullMetrics> metrics;
        List<SecFiling> filings;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<FullMetrics>> metricsFuture = submitSafely(
                    executor,
                    "calcul des métriques pour " + normalizedTicker,
                    () -> financialAnalysisService.computeMetrics(normalizedTicker),
                    List.of());
            CompletableFuture<List<SecFiling>> annualFilingsFuture = submitSafely(
                    executor,
                    "récupération des 10-K pour " + normalizedTicker,
                    () -> documentSource.fetchFilings(normalizedTicker, "10-K", 2),
                    List.of());
            CompletableFuture<List<SecFiling>> quarterlyFilingsFuture = submitSafely(
                    executor,
                    "récupération des 10-Q pour " + normalizedTicker,
                    () -> documentSource.fetchFilings(normalizedTicker, "10-Q", 4),
                    List.of());

            metrics = metricsFuture.join();
            filings = new ArrayList<>(annualFilingsFuture.join());
            filings.addAll(quarterlyFilingsFuture.join());
        }

        int processed = 0;
        int ignored = 0;
        int failed = 0;
        for (SecFiling filing : filings) {
            try {
                if (insightRepository.existsByAccessionNumber(filing.accessionNumber())) {
                    ignored++;
                    continue;
                }

                String cleanText = textCleaner.clean(filing.rawContent());
                var sections = sectionExtractor.extractSections(cleanText);
                var insight = aiSummaryService.summarize(normalizedTicker, filing, sections, metrics);
                insightRepository.save(normalizedTicker, insight);
                processed++;
            } catch (Exception exception) {
                failed++;
                log.warn("Échec du traitement du filing '{}' pour '{}'.",
                        filing == null ? "inconnu" : filing.accessionNumber(), normalizedTicker, exception);
            }
        }

        log.info("Ingestion SEC terminée pour '{}': {} traités, {} ignorés, {} en échec.",
                normalizedTicker, processed, ignored, failed);
    }

    private <T> CompletableFuture<List<T>> submitSafely(
            ExecutorService executor,
            String operation,
            Supplier<List<T>> supplier,
            List<T> fallback) {
        return CompletableFuture.supplyAsync(supplier, executor)
                .thenApply(result -> result == null ? fallback : result)
                .exceptionally(exception -> {
                    Throwable cause = exception instanceof CompletionException && exception.getCause() != null
                            ? exception.getCause()
                            : exception;
                    log.warn("Échec de l'opération '{}'; poursuite avec une liste vide.", operation, cause);
                    return fallback;
                });
    }
}