package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.IngestionStatus;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.domain.port.IIngestionTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSummaryServiceImplTest {

    @Mock
    private ICompanyInsightRepository insightRepository;

    @Mock
    private IDocumentSourcePort documentSource;

    @Mock
    private IIngestionTracker ingestionTracker;

    @Mock
    private IngestionLauncher ingestionLauncher;

    @InjectMocks
    private AiSummaryServiceImpl service;

    @Test
    void requestRefreshDelegatesToLauncherWithNormalizedTicker() {
        when(ingestionTracker.isIngesting("AAPL")).thenReturn(false);

        service.requestRefresh(" aapl ");

        verify(ingestionLauncher).refreshIfStale("AAPL");
    }

    @Test
    void requestRefreshSkipsWhenIngestionAlreadyRunning() {
        when(ingestionTracker.isIngesting("AAPL")).thenReturn(true);

        service.requestRefresh("AAPL");

        verify(ingestionLauncher, never()).refreshIfStale(any());
    }

    @Test
    void requestRefreshIgnoresBlankTicker() {
        service.requestRefresh(null);
        service.requestRefresh("");
        service.requestRefresh("   ");

        verifyNoInteractions(ingestionLauncher, ingestionTracker);
    }

    @Test
    void requestRefreshSwallowsLauncherExceptions() {
        doThrow(new IllegalStateException("launcher failed"))
                .when(ingestionLauncher).refreshIfStale("AAPL");

        assertThatCode(() -> service.requestRefresh("AAPL")).doesNotThrowAnyException();

        when(ingestionTracker.isIngesting("AAPL")).thenThrow(new RuntimeException("tracker failed"));
        assertThatCode(() -> service.requestRefresh("AAPL")).doesNotThrowAnyException();
    }

    @Test
    void requestIngestionStillUsesLaunchIfAllowed() {
        when(ingestionTracker.isIngesting("AAPL")).thenReturn(false);
        service.requestIngestion("AAPL");
        verify(ingestionLauncher, times(1)).launchIfAllowed("AAPL");

        when(ingestionTracker.isIngesting("AAPL")).thenReturn(true);
        service.requestIngestion("AAPL");
        verify(ingestionLauncher, times(1)).launchIfAllowed("AAPL");

        verify(ingestionLauncher, never()).refreshIfStale(any());
    }

    @Test
    void getStatusReportsReadyCountAndLatestGeneratedAt() {
        Instant earlier = Instant.parse("2025-03-01T12:00:00Z");
        Instant latest = Instant.parse("2025-06-01T00:00:00Z");
        when(insightRepository.findByTicker("AAPL"))
                .thenReturn(List.of(insight(earlier), insight(null), insight(latest)));
        when(ingestionTracker.isIngesting("AAPL")).thenReturn(false);

        IngestionStatus status = service.getStatus("AAPL");

        assertThat(status).isEqualTo(new IngestionStatus("AAPL", true, false, 3, latest));
    }

    @Test
    void getStatusIsNotReadyWithoutInsightsAndReflectsInProgress() {
        when(insightRepository.findByTicker("AAPL")).thenReturn(List.of());
        when(ingestionTracker.isIngesting("AAPL")).thenReturn(true);

        assertThat(service.getStatus("AAPL"))
                .isEqualTo(new IngestionStatus("AAPL", false, true, 0, null));
        assertThat(service.isReady("AAPL")).isFalse();
    }

    @Test
    void isReadyAndIsTickerSupportedNormalizeTicker() {
        when(insightRepository.findByTicker("AAPL")).thenReturn(List.of());
        when(ingestionTracker.isIngesting("AAPL")).thenReturn(false);
        when(documentSource.isAvailable("AAPL")).thenReturn(true);

        assertThat(service.isReady("aapl")).isFalse();
        assertThat(service.isTickerSupported("aapl")).isTrue();

        verify(insightRepository).findByTicker("AAPL");
        verify(documentSource).isAvailable("AAPL");
    }

    private CompanyInsight insight(Instant generatedAt) {
        return new CompanyInsight(
                "AAPL", "10-K", "FY 2024", "0000000000-25-000001",
                "Performance solide", "Marges stables", List.of("Risque de marché"),
                null, null, List.of("Fait marquant"), generatedAt);
    }
}
