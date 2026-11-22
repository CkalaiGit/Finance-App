package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.intelligence.domain.model.FilingReference;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import com.cairedine.finance.app.intelligence.domain.port.IInsightSynthesizer;
import com.cairedine.finance.app.intelligence.domain.port.IIngestionTracker;
import com.cairedine.finance.app.intelligence.domain.port.ISectionExtractor;
import com.cairedine.finance.app.intelligence.domain.port.ITextCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    private final IInsightSynthesizer insightSynthesizer;
    private final Duration delayBetweenCalls;
    private final int annualFilingsLimit;
    private final int quarterlyFilingsLimit;
    private final Set<String> ingestingTickers = ConcurrentHashMap.newKeySet();

    public DocumentIngestionService(
            IDocumentSourcePort documentSource,
            IFinancialAnalysisService financialAnalysisService,
            ICompanyInsightRepository insightRepository,
            ITextCleaner textCleaner,
            ISectionExtractor sectionExtractor,
            IInsightSynthesizer insightSynthesizer,
            @Value("${intelligence.summarization.delay-between-calls:5s}") Duration delayBetweenCalls,
            @Value("${intelligence.ingestion.annual-filings-limit:1}") int annualFilingsLimit,
            @Value("${intelligence.ingestion.quarterly-filings-limit:2}") int quarterlyFilingsLimit) {
        this.documentSource = documentSource;
        this.financialAnalysisService = financialAnalysisService;
        this.insightRepository = insightRepository;
        this.textCleaner = textCleaner;
        this.sectionExtractor = sectionExtractor;
        this.insightSynthesizer = insightSynthesizer;
        this.delayBetweenCalls = delayBetweenCalls;
        this.annualFilingsLimit = Math.max(0, annualFilingsLimit);
        this.quarterlyFilingsLimit = Math.max(0, quarterlyFilingsLimit);
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

        ReferenceScan scan = scanReferences(normalizedTicker);
        List<FilingReference> newReferences = scan.newReferences();
        if (newReferences.isEmpty()) {
            log.info("Ingestion SEC pour '{}': rien de nouveau ({} ignorés, {} en échec).",
                    normalizedTicker, scan.ignored(), scan.failed());
            return;
        }

        List<FullMetrics> metrics = computeMetricsSafely(normalizedTicker);
        int processed = 0;
        int failed = scan.failed();
        for (int index = 0; index < newReferences.size(); index++) {
            if (processReference(normalizedTicker, newReferences.get(index), metrics)) {
                processed++;
            } else {
                failed++;
            }
            if (index + 1 < newReferences.size() && !pauseBetweenFilings()) {
                break;
            }
        }

        log.info("Ingestion SEC terminée pour '{}': {} traités, {} ignorés, {} en échec.",
                normalizedTicker, processed, scan.ignored(), failed);
    }

    private List<FilingReference> listReferences(String ticker) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<FilingReference>> annualFuture = submitSafely(
                    executor,
                    "listing des 10-K pour " + ticker,
                    () -> documentSource.listFilings(ticker, "10-K", annualFilingsLimit),
                    List.of());
            CompletableFuture<List<FilingReference>> quarterlyFuture = submitSafely(
                    executor,
                    "listing des 10-Q pour " + ticker,
                    () -> documentSource.listFilings(ticker, "10-Q", quarterlyFilingsLimit),
                    List.of());

            List<FilingReference> references = new ArrayList<>(annualFuture.join());
            references.addAll(quarterlyFuture.join());
            return references;
        }
    }

    private ReferenceScan scanReferences(String ticker) {
        List<FilingReference> newReferences = new ArrayList<>();
        int ignored = 0;
        int failed = 0;
        for (FilingReference reference : listReferences(ticker)) {
            try {
                if (insightRepository.existsByAccessionNumber(reference.accessionNumber())) {
                    ignored++;
                } else {
                    newReferences.add(reference);
                }
            } catch (Exception exception) {
                failed++;
                log.warn("Échec de la vérification du filing '{}' pour '{}'.",
                        reference.accessionNumber(), ticker, exception);
            }
        }
        return new ReferenceScan(newReferences, ignored, failed);
    }

    /**
     * Télécharge, analyse et sauvegarde un filing nouveau.
     *
     * @return true si l'insight a été sauvegardé, false en cas d'échec (téléchargement vide ou exception)
     */
    private boolean processReference(String ticker, FilingReference reference, List<FullMetrics> metrics) {
        try {
            Optional<SecFiling> downloaded = documentSource.downloadFiling(ticker, reference);
            if (downloaded.isEmpty()) {
                return false;
            }
            SecFiling filing = downloaded.get();
            String cleanText = textCleaner.clean(filing.rawContent());
            var sections = sectionExtractor.extractSections(cleanText);
            List<FullMetrics> matchedMetrics = metricsForFiling(ticker, filing, metrics);
            var insight = insightSynthesizer.synthesize(ticker, filing, sections, matchedMetrics);
            insightRepository.save(ticker, insight);
            return true;
        } catch (Exception exception) {
            log.warn("Échec du traitement du filing '{}' pour '{}'.",
                    reference.accessionNumber(), ticker, exception);
            return false;
        }
    }

    private List<FullMetrics> computeMetricsSafely(String ticker) {
        try {
            List<FullMetrics> metrics = financialAnalysisService.computeMetrics(ticker);
            return metrics == null ? List.of() : metrics;
        } catch (Exception exception) {
            log.warn("Échec du calcul des métriques pour '{}'; poursuite sans métriques.", ticker, exception);
            return List.of();
        }
    }

    private List<FullMetrics> metricsForFiling(String ticker, SecFiling filing, List<FullMetrics> metrics) {
        LocalDate filingDate;
        try {
            filingDate = LocalDate.parse(filing.period());
        } catch (DateTimeParseException | NullPointerException exception) {
            log.warn("Période de filing SEC non analysable pour '{}': '{}'. Aucune métrique associée.",
                    ticker, filing.period(), exception);
            return List.of();
        }

        List<DatedMetrics> matchingMetrics = new ArrayList<>();
        for (FullMetrics metric : metrics) {
            if (metric == null || metric.fiscalYearEndDate() == null) {
                log.warn("Date d'exercice fiscal manquante pour '{}'; métrique ignorée.", ticker);
                continue;
            }
            try {
                LocalDate metricDate = LocalDate.parse(metric.fiscalYearEndDate());
                if (!metricDate.isAfter(filingDate)) {
                    matchingMetrics.add(new DatedMetrics(metricDate, metric));
                }
            } catch (DateTimeParseException exception) {
                log.warn("Date d'exercice fiscal non analysable pour '{}': '{}'; métrique ignorée.",
                        ticker, metric.fiscalYearEndDate(), exception);
            }
        }
        return matchingMetrics.stream()
                .sorted(Comparator.comparing(DatedMetrics::fiscalYearEndDate).reversed())
                .limit(3)
                .map(DatedMetrics::metrics)
                .toList();
    }

    private boolean pauseBetweenFilings() {
        if (delayBetweenCalls.isNegative() || delayBetweenCalls.isZero()) {
            return true;
        }
        try {
            Thread.sleep(delayBetweenCalls);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Pause entre les filings interrompue; ingestion interrompue.");
            return false;
        }
    }

    private record DatedMetrics(LocalDate fiscalYearEndDate, FullMetrics metrics) {
    }

    private record ReferenceScan(List<FilingReference> newReferences, int ignored, int failed) {
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
