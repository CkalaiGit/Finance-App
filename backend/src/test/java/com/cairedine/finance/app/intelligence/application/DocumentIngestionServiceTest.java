package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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
        SecFiling filing = filing("known-accession");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(List.of());
        when(documentSource.fetchFilings("AAPL", "10-K", 1)).thenReturn(List.of(filing));
        when(documentSource.fetchFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
        when(insightRepository.existsByAccessionNumber("known-accession")).thenReturn(true);

        ingestionService.ingestDocuments("AAPL");

        verify(insightRepository).existsByAccessionNumber("known-accession");
        verifyNoInteractions(textCleaner, sectionExtractor, insightSynthesizer);
    }

    @Test
    void failedFilingDoesNotPreventProcessingFollowingFilings() {
        SecFiling failedFiling = filing("failed-accession");
        SecFiling successfulFiling = filing("successful-accession");
        CompanyInsight insight = new CompanyInsight(
                "AAPL", "10-K", "FY 2025", "successful-accession",
                "summary", "analysis", List.of(), null, null, List.of(), Instant.now());
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(List.of());
        when(documentSource.fetchFilings("AAPL", "10-K", 1))
                .thenReturn(List.of(failedFiling, successfulFiling));
        when(documentSource.fetchFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
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
        when(documentSource.fetchFilings("AAPL", "10-K", 1)).thenReturn(List.of(filing));
        when(documentSource.fetchFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
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
        SecFiling filing = new SecFiling("bad-period", "10-K", "FY 2025",
                "raw-content", Instant.now());
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(List.of(metrics("2024-12-31")));
        when(documentSource.fetchFilings("AAPL", "10-K", 1)).thenReturn(List.of(filing));
        when(documentSource.fetchFilings("AAPL", "10-Q", 2)).thenReturn(List.of());
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var firstIngestion = executor.submit(() -> ingestionService.ingestDocuments("aapl"));
            assertThat(ingestionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(ingestionService.isIngesting("AAPL")).isTrue();

            ingestionService.ingestDocuments("AAPL");

            releaseIngestion.countDown();
            firstIngestion.get(5, TimeUnit.SECONDS);
            verify(documentSource).isAvailable("AAPL");
            verify(financialAnalysisService).computeMetrics("AAPL");
            verify(documentSource).fetchFilings("AAPL", "10-K", 1);
            verify(documentSource).fetchFilings("AAPL", "10-Q", 2);
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
