package com.cairedine.finance.app.financialanalysis;

import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.FullMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

        // Mock d'un JWT valide pour passer la sécurité
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "read")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer fake-token")
                .build();
    }

    @Test
    void shouldFetchAndPersistAnalysisE2E() {
        // Arrange
        String ticker = "GOOGL";

        // Act
        ResponseEntity<List<FullMetricsResponse>> response = restClient.get()
                .uri("/api/v1/analysis/{ticker}", ticker)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        List<FullMetricsResponse> bodyList = response.getBody();
        assertThat(bodyList).isNotEmpty();
        FullMetricsResponse body = bodyList.getFirst();

        // ── Growth
        assertThat(body.growth()).isNotNull();
        assertThat(body.growth().revenueGrowth3Y()).isNotNull();

        // ── Value
        assertThat(body.value()).isNotNull();
        assertThat(body.value().evToEbit()).isNotNull();

        // ── Quality
        assertThat(body.quality()).isNotNull();
        assertThat(body.quality().roic()).isNotNull();

        // ── Verify dates
        assertThat(body.fiscalYearEndDate()).isNotNull();
        assertThat(body.fiscalYearEndDate()).matches("\\d{4}-\\d{2}-\\d{2}");  // Format YYYY-MM-DD
        assertThat(body.marketDataAsOf()).isNotNull();
        assertThat(body.marketDataAsOf()).isCloseTo(Instant.now(), within(60, ChronoUnit.SECONDS));

        // Verify Persistence
        var persistedList = repository.findAllByTickerOrderByFiscalYearEndDateDesc(ticker);
        assertThat(persistedList).isNotEmpty();
        var persisted = persistedList.getFirst();
        assertThat(persisted.getTicker()).isEqualTo(ticker);
        assertThat(persisted.getFiscalYearEndDate()).isEqualTo(body.fiscalYearEndDate());
        assertThat(persisted.getMarketDataAsOf()).isNotNull();

        // ── Persistance checks
        assertThat(persisted.getGrowthMetrics().getRevenueGrowth3Y())
                .isEqualByComparingTo(body.growth().revenueGrowth3Y());
        assertThat(persisted.getValueMetrics().getEvToEbit())
                .isEqualByComparingTo(body.value().evToEbit());
    }

    @Test
    void shouldReturnCachedResultOnSecondCall() {
        String ticker = "GOOGL";

        // Premier appel — calcul + persistance
        ResponseEntity<List<FullMetricsResponse>> first = restClient.get()
                .uri("/api/v1/analysis/{ticker}", ticker)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();

        // Second appel — doit venir du cache
        ResponseEntity<List<FullMetricsResponse>> second = restClient.get()
                .uri("/api/v1/analysis/{ticker}", ticker)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();

        List<FullMetricsResponse> firstBodyList = first.getBody();
        List<FullMetricsResponse> secondBodyList = second.getBody();
        
        assertThat(firstBodyList).isNotEmpty();
        assertThat(secondBodyList).isNotEmpty();
        
        FullMetricsResponse firstBody = firstBodyList.getFirst();
        FullMetricsResponse secondBody = secondBodyList.getFirst();
        
        assertThat(secondBody.fiscalYearEndDate()).isEqualTo(firstBody.fiscalYearEndDate());
        assertThat(secondBody.growth().revenueGrowth3Y())
                .isEqualByComparingTo(firstBody.growth().revenueGrowth3Y());

        // Un seul enregistrement en base
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn404ForUnknownTicker() {
        try {
            restClient.get()
                    .uri("/api/v1/analysis/{ticker}", "TICKER_INEXISTANT")
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<FullMetricsResponse>>() {});

            fail("Une exception HTTP était attendue");

        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }
    }
}
