package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    private AiSummaryServiceImpl aiSummaryService;

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService(
                documentSource, financialAnalysisService, insightRepository,
                textCleaner, sectionExtractor, aiSummaryService);
    }

    @Test
    void unknownTickerDoesNotStartIngestionWork() {
        when(documentSource.isAvailable("UNKNOWN")).thenReturn(false);

        ingestionService.ingestDocuments("unknown");

        verify(documentSource).isAvailable("UNKNOWN");
        verifyNoInteractions(financialAnalysisService, insightRepository, textCleaner, sectionExtractor, aiSummaryService);
        assertThat(ingestionService.isIngesting("UNKNOWN")).isFalse();
    }

    @Test
    void knownFilingIsSkipped() {
        SecFiling filing = filing("known-accession");
        when(documentSource.isAvailable("AAPL")).thenReturn(true);
        when(financialAnalysisService.computeMetrics("AAPL")).thenReturn(List.of());
        when(documentSource.fetchFilings("AAPL", "10-K", 2)).thenReturn(List.of(filing));
        when(documentSource.fetchFilings("AAPL", "10-Q", 4)).thenReturn(List.of());
        when(insightRepository.existsByAccessionNumber("known-accession")).thenReturn(true);

        ingestionService.ingestDocuments("AAPL");

        verify(insightRepository).existsByAccessionNumber("known-accession");
        verifyNoInteractions(textCleaner, sectionExtractor, aiSummaryService);
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
        when(documentSource.fetchFilings("AAPL", "10-K", 2))
                .thenReturn(List.of(failedFiling, successfulFiling));
        when(documentSource.fetchFilings("AAPL", "10-Q", 4)).thenReturn(List.of());
        when(insightRepository.existsByAccessionNumber("failed-accession")).thenReturn(false);
        when(insightRepository.existsByAccessionNumber("successful-accession")).thenReturn(false);
        when(textCleaner.clean("raw-content")).thenReturn("clean-content");
        when(sectionExtractor.extractSections("clean-content")).thenReturn(List.of());
        when(aiSummaryService.summarize(eq("AAPL"), eq(failedFiling), anyList(), anyList()))
                .thenThrow(new IllegalStateException("summary failed"));
        when(aiSummaryService.summarize(eq("AAPL"), eq(successfulFiling), anyList(), anyList()))
                .thenReturn(insight);

        ingestionService.ingestDocuments("AAPL");

        verify(aiSummaryService).summarize(eq("AAPL"), eq(failedFiling), anyList(), anyList());
        verify(aiSummaryService).summarize(eq("AAPL"), eq(successfulFiling), anyList(), anyList());
        verify(insightRepository).save("AAPL", insight);
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
            verify(documentSource).fetchFilings("AAPL", "10-K", 2);
            verify(documentSource).fetchFilings("AAPL", "10-Q", 4);
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
        return new SecFiling(accession, "10-K", "FY 2025", "raw-content", Instant.now());
    }
}
