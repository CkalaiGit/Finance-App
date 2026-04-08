package com.cairedine.finance.app.financialanalysis;

import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.FullMetricsResponse;
import com.cairedine.finance.app.webclient.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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

    @MockitoBean
    private IMarketDataPort marketDataPort;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // 1. Mock du JWT pour passer la sécurité Spring
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        // 2. Mock des données de l'API (IMarketDataPort)

        // IncomeStatementRecord : symbol, date, revenue, operatingIncome, ebitda, eps, sga
        IncomeStatementRecord mockIncome = new IncomeStatementRecord(
                "GOOGL",
                "2023-12-31",
                new BigDecimal("307394000000"),
                new BigDecimal("84293000000"),
                new BigDecimal("95000000000"),
                new BigDecimal("5.80"),
                new BigDecimal("18000000000")
        );

        // Le service exige size >= 4 pour le calcul de croissance 3 ans
        when(marketDataPort.fetchIncomeStatements(eq("GOOGL"), anyInt()))
                .thenReturn(List.of(mockIncome, mockIncome, mockIncome, mockIncome));

        // CashFlowRecord : symbol, date, freeCashFlow, commonStockRepurchased
        CashFlowRecord mockCashFlow = new CashFlowRecord(
                "GOOGL",
                "2023-12-31",
                new BigDecimal("60000000000"),
                BigDecimal.ZERO
        );

        // IMPORTANT : Le service exige size >= 2 pour le calcul de croissance FCF
        when(marketDataPort.fetchCashFlowStatements(anyString(), anyInt()))
                .thenReturn(List.of(mockCashFlow, mockCashFlow));

        // KeyMetricsRecord : symbol, roic, netDebtToEbitda, enterpriseValueTTM, peRatioTTM, evToSalesTTM
        when(marketDataPort.fetchKeyMetricsTtm(anyString()))
                .thenReturn(Optional.of(new KeyMetricsRecord(
                        "GOOGL",
                        new BigDecimal("0.25"),
                        new BigDecimal("0.5"),
                        new BigDecimal("1500000000000"),
                        new BigDecimal("25"),
                        new BigDecimal("5")
                )));

        // AnalystEstimateRecord : symbol, date, epsAvg, revenueAvg
        when(marketDataPort.fetchAnalystEstimates(anyString()))
                .thenReturn(List.of(
                        new AnalystEstimateRecord("GOOGL", "2024-12-31", new BigDecimal("6.5"), new BigDecimal("320000000000")),
                        new AnalystEstimateRecord("GOOGL", "2023-12-31", new BigDecimal("5.8"), new BigDecimal("300000000000"))
                ));

        // 3. Initialisation du RestClient de test
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
        // Arrange
        String unknownTicker = "TICKER_INEXISTANT";
        when(marketDataPort.fetchIncomeStatements(eq(unknownTicker), anyInt()))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> restClient.get()
                .uri("/api/v1/analysis/{ticker}", unknownTicker)
                .retrieve()
                .toBodilessEntity()) // Plus d'accolades, plus de ; interne
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(ex -> {
                    HttpClientErrorException httpEx = (HttpClientErrorException) ex;
                    assertThat(httpEx.getStatusCode().value()).isEqualTo(404);
                });
    }
}
