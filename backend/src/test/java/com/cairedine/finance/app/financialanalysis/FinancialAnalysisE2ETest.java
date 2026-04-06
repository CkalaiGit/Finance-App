package com.cairedine.finance.app.financialanalysis;

import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.FullMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class FinancialAnalysisE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private IFinancialAnalysisRepository repository;

    private RestClient restClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldFetchAndPersistAnalysisE2E() {
        // Arrange
        String ticker = "GOOGL";

        // Act
        ResponseEntity<FullMetricsResponse> response = restClient.get()
                .uri("/api/v1/analysis/{ticker}", ticker)
                .retrieve()
                .toEntity(FullMetricsResponse.class);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        FullMetricsResponse body = response.getBody();
        assertThat(body).isNotNull();

        // ── Growth
        assertThat(body.growth()).isNotNull();
        assertThat(body.growth().revenueGrowth3Y()).isNotNull();

        // ── Value
        assertThat(body.value()).isNotNull();
        assertThat(body.value().evToEbit()).isNotNull();
        assertThat(body.value().peRatioTTM()).isNotNull();
        assertThat(body.value().pegRatioForward()).isNotNull();
        assertThat(body.value().evToSales()).isNotNull();

        // ── Quality
        assertThat(body.quality()).isNotNull();
        assertThat(body.quality().roic()).isNotNull();
        assertThat(body.quality().operatingMargin()).isNotNull();
        assertThat(body.quality().netDebtToEbitda()).isNotNull();
        assertThat(body.quality().freeCashFlowMargin()).isNotNull();
        assertThat(body.quality().sgaToRevenue()).isNotNull();

        // Verify Persistence (Cache-Aside)
        var persisted = repository.findByTicker(ticker);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getTicker()).isEqualTo(ticker);

        // ── Persistance growth
        assertThat(persisted.get().getGrowthMetrics().getRevenueGrowth3Y())
                .isEqualByComparingTo(body.growth().revenueGrowth3Y());

        // ── Persistance value
        assertThat(persisted.get().getValueMetrics()).isNotNull();
        assertThat(persisted.get().getValueMetrics().getEvToEbit())
                .isEqualByComparingTo(body.value().evToEbit());
        assertThat(persisted.get().getValueMetrics().getPeRatioTTM())
                .isEqualByComparingTo(body.value().peRatioTTM());
        assertThat(persisted.get().getValueMetrics().getPegRatioForward())
                .isEqualByComparingTo(body.value().pegRatioForward());
        assertThat(persisted.get().getValueMetrics().getEvToSales())
                .isEqualByComparingTo(body.value().evToSales());

        // ── Persistance quality
        assertThat(persisted.get().getQualityMetrics()).isNotNull();
        assertThat(persisted.get().getQualityMetrics().getRoic())
                .isEqualByComparingTo(body.quality().roic());
        assertThat(persisted.get().getQualityMetrics().getOperatingMargin())
                .isEqualByComparingTo(body.quality().operatingMargin());
        assertThat(persisted.get().getQualityMetrics().getNetDebtToEbitda())
                .isEqualByComparingTo(body.quality().netDebtToEbitda());
        assertThat(persisted.get().getQualityMetrics().getFreeCashFlowMargin())
                .isEqualByComparingTo(body.quality().freeCashFlowMargin());
        assertThat(persisted.get().getQualityMetrics().getSgaToRevenue())
                .isEqualByComparingTo(body.quality().sgaToRevenue());
    }

    @Test
    void shouldReturnCachedResultOnSecondCall() {
        String ticker = "GOOGL";

        // Premier appel — calcul + persistance
        ResponseEntity<FullMetricsResponse> first = restClient.get()
                .uri("/api/v1/analysis/{ticker}", ticker)
                .retrieve()
                .toEntity(FullMetricsResponse.class);

        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();

        // Second appel — doit venir du cache
        ResponseEntity<FullMetricsResponse> second = restClient.get()
                .uri("/api/v1/analysis/{ticker}", ticker)
                .retrieve()
                .toEntity(FullMetricsResponse.class);

        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();

        assert first.getBody() != null;
        assert second.getBody() != null;
        assertThat(second.getBody().growth().revenueGrowth3Y())
                .isEqualByComparingTo(first.getBody().growth().revenueGrowth3Y());
        assertThat(second.getBody().value().evToEbit())
                .isEqualByComparingTo(first.getBody().value().evToEbit());
        assertThat(second.getBody().quality().roic())
                .isEqualByComparingTo(first.getBody().quality().roic());

        // Un seul enregistrement en base — pas de doublon
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn404ForUnknownTicker() {
        try {
            restClient.get()
                    .uri("/api/v1/analysis/{ticker}", "SYMBOLINEXISTANT")
                    .retrieve()
                    .toEntity(FullMetricsResponse.class);

            fail("Une exception HTTP était attendue");

        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }
    }
}
