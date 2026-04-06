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

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(body.growth()).isNotNull();

        // Verify Persistence (Cache-Aside)
        var persisted = repository.findByTicker(ticker);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getGrowthMetrics().getRevenueGrowth3Y()).isEqualByComparingTo(body.growth().revenueGrowth3Y());
        assertThat(persisted.get().getTicker()).isEqualTo(ticker);
    }
}
