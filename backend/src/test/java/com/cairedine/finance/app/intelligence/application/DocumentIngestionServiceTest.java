package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.FilingReference;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import com.cairedine.finance.app.intelligence.domain.port.IInsightSynthesizer;
import com.cairedine.finance.app.intelligence.domain.port.ISectionExtractor;
import com.cairedine.finance.app.intelligence.domain.port.ITextCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private IDocumentSourcePort documentSource;
    @Mock
    private IFinancialAnalysisService financialAnalysisService;
    @Mock
    private ICompanyInsightRepository insightRepository;
    @Mock
    private ITextCleaner textCleaner;
    @Mock
    private ISectionExtractor sectionExtractor;
    @Mock
    private IInsightSynthesizer insightSynthesizer;

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService(
                documentSource, financialAnalysisService, insightRepository,
                textCleaner, sectionExtractor, insightSynthesizer, java.time.Duration.ZERO, 1, 2);
    }

    @Test
    void unknownTickerDoesNotStartIngestionWork() {
        when(documentSource.isAvailable("UNKNOWN")).thenReturn(false);

        ingestionService.ingestDocuments("unknown");

        verify(documentSource).isAvailable("UNKNOWN");
        verifyNoInteractions(financialAnalysisService, insightRepository, textCleaner, sectionExtractor, insightSynthesizer);
        assertThat(ingestionService.isIngesting("UNKNOWN")).isFalse();
    }

    @Test
    void knownFilingIsSkipped() {
        FilingReference known = reference("known-accession", "10-K", "2025-12-31");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(known));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(insightRepository.existsByAccessionNumber("known-accession")).thenReturn(true);

        ingestionService.ingestDocuments("AAPL");

        verify(insightRepository).existsByAccessionNumber("known-accession");
        verify(documentSource, never()).downloadFiling(any(), any());
        verifyNoInteractions(textCleaner, sectionExtractor, insightSynthesizer);
    }

    @Test
    void failedFilingDoesNotPreventProcessingFollowingFilings() {
        FilingReference failedReference = reference("failed-accession", "10-K", "2025-12-31");
        FilingReference successfulReference = reference("successful-accession", "10-K", "2025-12-31");
        SecFiling failedFiling = filing("failed-accession");
        SecFiling successfulFiling = filing("successful-accession");
        CompanyInsight insight = new CompanyInsight(
                "AAPL", "10-K", "FY 2025", "successful-accession",
                "summary", "analysis", List.of(), null, null, List.of(), Instant.now());
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(List.of());
        when(documentSource.listFilings("AAPL", "10-K", 1))
                .thenReturn(List.of(failedReference, successfulReference));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(documentSource.downloadFiling("AAPL", failedReference)).thenReturn(Optional.of(failedFiling));
        when(documentSource.downloadFiling("AAPL", successfulReference)).thenReturn(Optional.of(successfulFiling));
        when(insightRepository.existsByAccessionNumber("failed-accession")).thenReturn(false);
        when(insightRepository.existsByAccessionNumber("successful-accession")).thenReturn(false);
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(failedFiling), anyList(), anyList()))
                .thenThrow(new IllegalStateException("summary failed"));
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(successfulFiling), anyList(), anyList()))
                .thenReturn(insight);

        ingestionService.ingestDocuments("AAPL");

        verify(insightSynthesizer).synthesize(eq("AAPL"), eq(failedFiling), anyList(), anyList());
        verify(insightSynthesizer).synthesize(eq("AAPL"), eq(successfulFiling), anyList(), anyList());
        verify(insightRepository).save("AAPL", insight);
    }

    @Test
    void onlyMetricsForTheFilingPeriodAndEarlierArePassedMostRecentFirst() {
        FilingReference reference = reference("filing-accession", "10-K", "2024-06-30");
        SecFiling filing = new SecFiling("filing-accession", "10-K", "2024-06-30",
                "raw-content", Instant.now());
        List<FullMetrics> metrics = List.of(
                metrics("2025-12-31"),
                metrics("2024-06-30"),
                metrics("2023-12-31"),
                metrics("2022-12-31"),
                metrics("2021-12-31"));
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(metrics);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(reference));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(documentSource.downloadFiling("AAPL", reference)).thenReturn(Optional.of(filing));
        when(insightRepository.existsByAccessionNumber("filing-accession")).thenReturn(false);
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(filing), anyList(), anyList()))
                .thenReturn(insight("filing-accession"));

        ingestionService.ingestDocuments("AAPL");

        verify(insightSynthesizer).synthesize(eq("AAPL"), eq(filing), anyList(),
                argThat(matched -> matched.stream().map(FullMetrics::fiscalYearEndDate).toList()
                        .equals(List.of("2024-06-30", "2023-12-31", "2022-12-31"))));
    }

    @Test
    void unparseableFilingPeriodPassesNoMetrics() {
        FilingReference reference = reference("bad-period", "10-K", "FY 2025");
        SecFiling filing = new SecFiling("bad-period", "10-K", "FY 2025",
                "raw-content", Instant.now());
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(List.of(metrics("2024-12-31")));
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(reference));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(documentSource.downloadFiling("AAPL", reference)).thenReturn(Optional.of(filing));
        when(insightRepository.existsByAccessionNumber("bad-period")).thenReturn(false);
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(filing), anyList(), anyList()))
                .thenReturn(insight("bad-period"));

        ingestionService.ingestDocuments("AAPL");

        verify(insightSynthesizer).synthesize("AAPL", filing, List.of(), List.of());
    }

    @Test
    void concurrentCallsForSameTickerRunOnlyOnce() throws Exception {
        CountDownLatch ingestionStarted = new CountDownLatch(1);
        CountDownLatch releaseIngestion = new CountDownLatch(1);
        when(documentSource.isAvailable("AAPL")).thenAnswer(_ -> {
            ingestionStarted.countDown();
            if (!releaseIngestion.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release ingestion");
            }
            return true;
        });
        when(documentSource.listFilings("AAPL", "10-K", 1))
                .thenReturn(List.of(reference("concurrent-accession", "10-K", "2025-12-31")));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var firstIngestion = executor.submit(() -> ingestionService.ingestDocuments("aapl"));
            assertThat(ingestionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(ingestionService.isIngesting("AAPL")).isTrue();

            ingestionService.ingestDocuments("AAPL");

            releaseIngestion.countDown();
            firstIngestion.get(5, TimeUnit.SECONDS);
            verify(documentSource).isAvailable("AAPL");
            verify(documentSource).listFilings("AAPL", "10-K", 1);
            verify(documentSource).listFilings("AAPL", "10-Q", 2);
            verify(financialAnalysisService).computeMetrics("AAPL");
            assertThat(ingestionService.isIngesting("AAPL")).isFalse();
        } finally {
            releaseIngestion.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void tickerIsRemovedFromTrackerAfterException() {
        when(documentSource.isAvailable("AAPL")).thenThrow(new IllegalStateException("source failed"));

        assertThatThrownBy(() -> ingestionService.ingestDocuments("AAPL"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("source failed");

        assertThat(ingestionService.isIngesting("AAPL")).isFalse();
    }

    @Test
    void nothingNewDoesNotDownloadComputeMetricsOrSynthesize() {
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1))
                .thenReturn(List.of(reference("known-annual", "10-K", "2025-12-31")));
        when(documentSource.listFilings("AAPL", "10-Q", 2))
                .thenReturn(List.of(reference("known-quarterly", "10-Q", "2025-08-01")));
        when(insightRepository.existsByAccessionNumber("known-annual")).thenReturn(true);
        when(insightRepository.existsByAccessionNumber("known-quarterly")).thenReturn(true);

        ingestionService.ingestDocuments("AAPL");

        verify(documentSource, never()).downloadFiling(any(), any());
        verify(financialAnalysisService, never()).computeMetrics(any());
        verifyNoInteractions(textCleaner, sectionExtractor, insightSynthesizer);
        verify(insightRepository, never()).save(any(), any());
    }

    @Test
    void onlyNewFilingsAreDownloadedAndSynthesized() {
        FilingReference newReference = reference("new-accession", "10-K", "2025-12-31");
        FilingReference knownReference = reference("known-accession", "10-K", "2024-12-31");
        SecFiling newFiling = filing("new-accession");
        CompanyInsight insight = insight("new-accession");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(newReference, knownReference));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(insightRepository.existsByAccessionNumber("new-accession")).thenReturn(false);
        when(insightRepository.existsByAccessionNumber("known-accession")).thenReturn(true);
        when(documentSource.downloadFiling("AAPL", newReference)).thenReturn(Optional.of(newFiling));
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(newFiling), anyList(), anyList())).thenReturn(insight);

        ingestionService.ingestDocuments("AAPL");

        verify(documentSource).downloadFiling("AAPL", newReference);
        verify(documentSource, never()).downloadFiling("AAPL", knownReference);
        verify(insightSynthesizer).synthesize(eq("AAPL"), eq(newFiling), anyList(), anyList());
        verify(insightRepository).save("AAPL", insight);
    }

    @Test
    void metricsAreComputedExactlyOnceWhenThereIsSomethingNew() {
        FilingReference first = reference("first-accession", "10-K", "2025-12-31");
        FilingReference second = reference("second-accession", "10-Q", "2025-08-01");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(first));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of(second));
        when(insightRepository.existsByAccessionNumber(anyString())).thenReturn(false);
        when(documentSource.downloadFiling(eq("AAPL"), any())).thenReturn(Optional.of(filing("accession")));
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), any(), anyList(), anyList()))
                .thenReturn(insight("accession"));

        ingestionService.ingestDocuments("AAPL");

        verify(financialAnalysisService, times(1)).computeMetrics("AAPL");
        verify(documentSource, times(2)).downloadFiling(eq("AAPL"), any());
    }

    @Test
    void listingUsesConfiguredLimits() {
        DocumentIngestionService service = new DocumentIngestionService(
                documentSource, financialAnalysisService, insightRepository,
                textCleaner, sectionExtractor, insightSynthesizer, java.time.Duration.ZERO, 1, 2);
        when(documentSource.isAvailable("AAPL")).thenReturn(true);

        service.ingestDocuments("AAPL");

        verify(documentSource).listFilings("AAPL", "10-K", 1);
        verify(documentSource).listFilings("AAPL", "10-Q", 2);
    }

    @Test
    void failedDownloadDoesNotPreventFollowingFilings() {
        FilingReference failedReference = reference("failed-download", "10-K", "2025-12-31");
        FilingReference successfulReference = reference("successful-download", "10-K", "2025-12-31");
        SecFiling successfulFiling = filing("successful-download");
        CompanyInsight insight = insight("successful-download");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1))
                .thenReturn(List.of(failedReference, successfulReference));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(documentSource.downloadFiling("AAPL", failedReference)).thenReturn(Optional.empty());
        when(documentSource.downloadFiling("AAPL", successfulReference)).thenReturn(Optional.of(successfulFiling));
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(successfulFiling), anyList(), anyList())).thenReturn(insight);

        ingestionService.ingestDocuments("AAPL");

        verify(insightSynthesizer, times(1)).synthesize(anyString(), any(), anyList(), anyList());
        verify(insightRepository, times(1)).save("AAPL", insight);
    }

    @Test
    void annualListingFailureStillProcessesQuarterly() {
        FilingReference quarterly = reference("quarterly-accession", "10-Q", "2025-08-01");
        SecFiling quarterlyFiling = new SecFiling("quarterly-accession", "10-Q", "2025-08-01",
                "raw-content", Instant.now());
        CompanyInsight insight = insight("quarterly-accession");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenThrow(new IllegalStateException("SEC down"));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of(quarterly));
        when(documentSource.downloadFiling("AAPL", quarterly)).thenReturn(Optional.of(quarterlyFiling));
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(insightSynthesizer.synthesize(eq("AAPL"), eq(quarterlyFiling), anyList(), anyList())).thenReturn(insight);

        ingestionService.ingestDocuments("AAPL");

        verify(insightRepository).save("AAPL", insight);
    }

    @Test
    void listingFailureOnBothFormsDoesNothing() {
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenThrow(new IllegalStateException("SEC down"));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());

        ingestionService.ingestDocuments("AAPL");

        verify(financialAnalysisService, never()).computeMetrics(any());
        verify(documentSource, never()).downloadFiling(any(), any());
        verify(insightRepository, never()).save(any(), any());
    }

    @Test
    void knownReferencesDoNotTriggerThePauseBetweenFilings() {
        // Test volontairement tolérant : on vérifie seulement qu'aucune pause de 2 secondes
        // n'est déclenchée pour la référence connue, avec une marge d'une seconde, afin
        // d'éviter toute instabilité sur une machine chargée.
        DocumentIngestionService service = new DocumentIngestionService(
                documentSource, financialAnalysisService, insightRepository,
                textCleaner, sectionExtractor, insightSynthesizer, java.time.Duration.ofSeconds(2), 1, 2);
        FilingReference known = reference("known-accession", "10-K", "2025-12-31");
        FilingReference fresh = reference("fresh-accession", "10-K", "2024-12-31");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(known, fresh));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(insightRepository.existsByAccessionNumber("known-accession")).thenReturn(true);
        when(insightRepository.existsByAccessionNumber("fresh-accession")).thenReturn(false);

        long startedAt = System.nanoTime();
        service.ingestDocuments("AAPL");
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        verify(documentSource, never()).downloadFiling("AAPL", known);
        assertThat(elapsedMillis).isLessThan(1_000L);
    }

    @Test
    void downloadReturningEmptyForAllKeepsTrackerClean() {
        FilingReference first = reference("first-download", "10-K", "2025-12-31");
        FilingReference second = reference("second-download", "10-Q", "2025-08-01");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(documentSource.listFilings("AAPL", "10-K", 1)).thenReturn(List.of(first));
        when(documentSource.listFilings("AAPL", "10-Q", 2)).thenReturn(List.of(second));
        when(insightRepository.existsByAccessionNumber(anyString())).thenReturn(false);
        when(documentSource.downloadFiling(eq("AAPL"), any())).thenReturn(Optional.empty());

        assertThatCode(() -> ingestionService.ingestDocuments("AAPL")).doesNotThrowAnyException();

        verify(documentSource, times(2)).downloadFiling(eq("AAPL"), any());
        verifyNoInteractions(textCleaner, sectionExtractor, insightSynthesizer);
        assertThat(ingestionService.isIngesting("AAPL")).isFalse();
    }

    private FilingReference reference(String accession, String formType, String filingDate) {
        return new FilingReference(accession, formType, filingDate, "primary.htm");
    }

    private SecFiling filing(String accession) {
        return new SecFiling(accession, "10-K", "2025-12-31", "raw-content", Instant.now());
    }

    private FullMetrics metrics(String date) {
        return new FullMetrics(
                new GrowthMetrics(null, null, null, null),
                new ValueMetrics(null, null, null, null),
                new QualityMetrics(null, null, null, null, null),
                date,
                Instant.now());
    }

    private CompanyInsight insight(String accession) {
        return new CompanyInsight(
                "AAPL", "10-K", "2025-12-31", accession,
                "summary", "analysis", List.of(), null, null, List.of(), Instant.now());
    }
}
