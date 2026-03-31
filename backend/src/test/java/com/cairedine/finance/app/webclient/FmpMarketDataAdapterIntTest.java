package com.cairedine.finance.app.webclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
class FmpMarketDataAdapterIntTest {

    @Autowired
    private IMarketDataPort marketDataPort;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("doit retourner un CompanyProfileRecord pour AAPL")
    void doitRetournerCompanyProfilePourAAPL() {
        Optional<CompanyProfileRecord> result = marketDataPort.fetchCompanyProfile("AAPL");

        assertThat(result).hasValueSatisfying(profile -> {
            assertThat(profile.symbol()).isEqualTo("AAPL");
            assertThat(profile.beta()).isPositive();
        });
    }

    @Test
    @DisplayName("doit retourner une liste de 4 IncomeStatementRecords annuels pour AAPL")
    void doitRetournerIncomeStatementsAnnuelsPourAAPL() {
        List<IncomeStatementRecord> results = marketDataPort.fetchIncomeStatements("AAPL", 4);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(4);
        assertThat(results.getFirst().revenue()).isPositive();
    }

    @Test
    @DisplayName("doit retourner un CashFlowRecord TTM pour AAPL")
    void doitRetournerCashFlowTtmPourAAPL() {
        Optional<CashFlowRecord> result = marketDataPort.fetchCashFlowTtm("AAPL");
        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo("AAPL");
        assertThat(result.get().freeCashFlow()).isPositive();
    }


    @Test
    @DisplayName("doit retourner une liste d'AnalystEstimateRecord pour AAPL")
    void doitRetournerAnalystEstimatesPourAAPL() {
        List<AnalystEstimateRecord> results = marketDataPort.fetchAnalystEstimates("AAPL");

        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().symbol()).isEqualTo("AAPL");
        assertThat(results.getFirst().epsAvg()).isPositive();
    }

    @Test
    @DisplayName("doit retourner un KeyMetricsRecord pour AAPL")
    void doitRetournerKeyMetricsPourAAPL() {
        Optional<KeyMetricsRecord> result = marketDataPort.fetchKeyMetricsTtm("AAPL");

        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo("AAPL");
        assertThat(result.get().returnOnInvestedCapitalTTM()).isPositive();
    }

    @Test
    @DisplayName("doit retourner vide pour un symbol inexistant")
    void doitRetournerVidePourSymbolInexistant() {
        Optional<CompanyProfileRecord> result = marketDataPort.fetchCompanyProfile("SYMBOLINEXISTANT");
        assertThat(result).isEmpty();
    }
}