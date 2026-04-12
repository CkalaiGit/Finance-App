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
        // Données avec croissance réelle (10% par an) pour montrer une évolution de 2020 à 2023
        List<IncomeStatementRecord> incomeStatements = List.of(
                // 2023 - année actuelle (valeurs finales)
                new IncomeStatementRecord(
                        "GOOGL",
                        "2023-12-31",
                        new BigDecimal("307394000000"),  // Revenue 2023
                        new BigDecimal("95000000000"),   // Operating Income 2023
                        new BigDecimal("106000000000"),  // EBITDA 2023
                        new BigDecimal("5.80"),          // EPS 2023
                        new BigDecimal("18000000000")    // SGA 2023
                ),
                // 2022
                new IncomeStatementRecord(
                        "GOOGL",
                        "2022-12-31",
                        new BigDecimal("282836000000"),  // Revenue 2022 (307394 * 0.92)
                        new BigDecimal("86363000000"),   // Operating Income 2022
                        new BigDecimal("96363000000"),   // EBITDA 2022
                        new BigDecimal("5.29"),          // EPS 2022
                        new BigDecimal("16363000000")    // SGA 2022
                ),
                // 2021
                new IncomeStatementRecord(
                        "GOOGL",
                        "2021-12-31",
                        new BigDecimal("257124000000"),  // Revenue 2021 (282836 * 0.91)
                        new BigDecimal("78488000000"),   // Operating Income 2021
                        new BigDecimal("87488000000"),   // EBITDA 2021
                        new BigDecimal("4.81"),          // EPS 2021
                        new BigDecimal("14851000000")    // SGA 2021
                ),
                // 2020 - année de référence (valeurs initiales)
                new IncomeStatementRecord(
                        "GOOGL",
                        "2020-12-31",
                        new BigDecimal("232995000000"),  // Revenue 2020 (257124 * 0.91)
                        new BigDecimal("71343000000"),   // Operating Income 2020
                        new BigDecimal("79343000000"),   // EBITDA 2020
                        new BigDecimal("4.37"),          // EPS 2020
                        new BigDecimal("13486000000")    // SGA 2020
                )
        );

        when(marketDataPort.fetchIncomeStatements(eq("GOOGL"), anyInt()))
                .thenReturn(incomeStatements);

        // CashFlowRecord : symbol, date, freeCashFlow, commonStockRepurchased
        // Données avec croissance pour FCF également
        List<CashFlowRecord> cashFlowStatements = List.of(
                // 2023
                new CashFlowRecord(
                        "GOOGL",
                        "2023-12-31",
                        new BigDecimal("76000000000"),   // FCF 2023
                        BigDecimal.ZERO
                ),
                // 2022
                new CashFlowRecord(
                        "GOOGL",
                        "2022-12-31",
                        new BigDecimal("69090000000"),   // FCF 2022 (76000 * 0.91)
                        BigDecimal.ZERO
                )
        );

        when(marketDataPort.fetchCashFlowStatements(anyString(), anyInt()))
                .thenReturn(cashFlowStatements);

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
                        new AnalystEstimateRecord("GOOGL", "2024-12-31", new BigDecimal("6.5"), new BigDecimal("338634000000")),
                        new AnalystEstimateRecord("GOOGL", "2023-12-31", new BigDecimal("5.8"), new BigDecimal("307394000000"))
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

        // ── Growth (doit avoir des valeurs > 0 avec le JDD de croissance 9% par an)
        assertThat(body.growth()).isNotNull();
        assertThat(body.growth().revenueGrowth3Y()).isNotNull();
        assertThat(body.growth().revenueGrowth3Y()).isGreaterThan(BigDecimal.ZERO);
        assertThat(body.growth().ebitdaGrowth3Y()).isGreaterThan(BigDecimal.ZERO);
        assertThat(body.growth().epsGrowth3Y()).isGreaterThan(BigDecimal.ZERO);

        // ── Value
        assertThat(body.value()).isNotNull();
        assertThat(body.value().evToEbit()).isNotNull();
        // EV=1.5T, EBIT=95B → ratio = 15789
        assertThat(body.value().evToEbit()).isGreaterThan(BigDecimal.ZERO);

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
