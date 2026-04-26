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
    @DisplayName("doit retourner un CompanyProfileRecord pour GOOGL")
    void doitRetournerCompanyProfilePourGOOGL() {
        Optional<CompanyProfileRecord> result = marketDataPort.fetchCompanyProfile("GOOGL");

        assertThat(result).hasValueSatisfying(profile -> {
            assertThat(profile.symbol()).isEqualTo("GOOGL");
            assertThat(profile.beta()).isPositive();
            assertThat(profile.industry()).isNotBlank();
            assertThat(profile.description()).isNotBlank();
            assertThat(profile.price()).isPositive();
        });
    }

    @Test
    @DisplayName("doit retourner une liste de 4 IncomeStatementRecords annuels pour GOOGL")
    void doitRetournerIncomeStatementsAnnuelsPourGOOGL() {
        List<IncomeStatementRecord> results = marketDataPort.fetchIncomeStatements("GOOGL", 4);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(4);
        assertThat(results.getFirst().revenue()).isPositive();
    }

    @Test
    @DisplayName("doit retourner un CashFlowRecord TTM pour GOOGL")
    void doitRetournerCashFlowTtmPourGOOGL() {
        Optional<CashFlowRecord> result = marketDataPort.fetchCashFlowTtm("GOOGL");
        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo("GOOGL");
        assertThat(result.get().freeCashFlow()).isPositive();
    }


    @Test
    @DisplayName("doit retourner une liste d'AnalystEstimateRecord pour GOOGL")
    void doitRetournerAnalystEstimatesPourGOOGL() {
        List<AnalystEstimateRecord> results = marketDataPort.fetchAnalystEstimates("GOOGL");

        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().symbol()).isEqualTo("GOOGL");
        assertThat(results.getFirst().epsAvg()).isPositive();
    }

    @Test
    @DisplayName("doit retourner un KeyMetricsRecord pour GOOGL")
    void doitRetournerKeyMetricsPourGOOGL() {
        Optional<KeyMetricsRecord> result = marketDataPort.fetchKeyMetricsTtm("GOOGL");

        assertThat(result).isPresent();
        assertThat(result.get().symbol()).isEqualTo("GOOGL");
        assertThat(result.get().returnOnInvestedCapitalTTM()).isPositive();
    }

    @Test
    @DisplayName("doit retourner vide pour un symbol inexistant")
    void doitRetournerVidePourSymbolInexistant() {
        Optional<CompanyProfileRecord> result = marketDataPort.fetchCompanyProfile("SYMBOLINEXISTANT");
        assertThat(result).isEmpty();
    }
}