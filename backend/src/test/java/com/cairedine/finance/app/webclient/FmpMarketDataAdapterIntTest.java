package com.cairedine.finance.app.webclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FmpMarketDataAdapterIntTest {

    @Autowired
    private IMarketDataPort marketDataPort;

    @Test
    @DisplayName("doit retourner un CompanyProfileRecord pour AAPL")
    void doitRetournerCompanyProfilePourAAPL() {
        CompanyProfileRecord result = marketDataPort.fetchCompanyProfile("AAPL");

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.beta()).isPositive();
        assertThat(result.marketCap()).isPositive();
    }

    @Test
    @DisplayName("doit retourner un IncomeStatementRecord TTM pour AAPL")
    void doitRetournerIncomeStatementTtmPourAAPL() {
        IncomeStatementRecord result = marketDataPort.fetchIncomeStatement("AAPL");
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.revenue()).isPositive();
        assertThat(result.operatingIncome()).isPositive();
        assertThat(result.eps()).isPositive();
    }

    @Test
    @DisplayName("doit retourner une liste de 4 IncomeStatementRecords annuels pour AAPL")
    void doitRetournerIncomeStatementsAnnuelsPourAAPL() {
        List<IncomeStatementRecord> results = marketDataPort.fetchIncomeStatements("AAPL", 4);

        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(4);
        assertThat(results.getFirst().revenue()).isPositive();
    }
}