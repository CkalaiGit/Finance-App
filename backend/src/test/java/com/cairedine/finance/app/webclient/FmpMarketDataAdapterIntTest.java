package com.cairedine.finance.app.webclient;

import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;

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
        assertThatNoException().isThrownBy(() -> marketDataPort.fetchCompanyProfile("AAPL"));
    }

    @Test
    @DisplayName("doit retourner une liste de 4 IncomeStatementRecords annuels pour AAPL")
    void doitRetournerIncomeStatementsAnnuelsPourAAPL() {
        List<IncomeStatementRecord> results = marketDataPort.fetchIncomeStatements("AAPL", 4);

        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(4);
        assertThat(results.getFirst().revenue()).isPositive();
        assertThatNoException().isThrownBy(() -> marketDataPort.fetchIncomeStatements("AAPL", 4));

    }

    @Test
    @DisplayName("doit retourner un CashFlowRecord TTM pour AAPL")
    void doitRetournerCashFlowTtmPourAAPL() {
        CashFlowRecord result = marketDataPort.fetchCashFlowTtm("AAPL");
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.freeCashFlow()).isPositive();
        assertThatNoException().isThrownBy(() -> marketDataPort.fetchCashFlowTtm("AAPL"));

    }


    @Test
    @DisplayName("doit retourner une liste d'AnalystEstimateRecord pour AAPL")
    void doitRetournerAnalystEstimatesPourAAPL() {
        List<AnalystEstimateRecord> results = marketDataPort.fetchAnalystEstimates("AAPL");

        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().symbol()).isEqualTo("AAPL");
        assertThat(results.getFirst().epsAvg()).isPositive();
        assertThatNoException().isThrownBy(() -> marketDataPort.fetchAnalystEstimates("AAPL"));

    }

    @Test
    @DisplayName("doit retourner un KeyMetricsRecord pour AAPL")
    void doitRetournerKeyMetricsPourAAPL() {
        KeyMetricsRecord result = marketDataPort.fetchKeyMetricsTtm("AAPL");

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("AAPL");
        assertThat(result.returnOnInvestedCapitalTTM()).isPositive();
        assertThat(result.evToEbit()).isPositive();
        assertThatNoException().isThrownBy(() -> marketDataPort.fetchKeyMetricsTtm("AAPL"));

    }

    @Test
    @DisplayName("doit lever TickerNotFoundException pour un symbol inexistant")
    void doitLeverTickerNotFoundExceptionPourSymbolInexistant() {
        assertThatThrownBy(() -> marketDataPort.fetchCompanyProfile("SYMBOLINEXISTANT"))
                .isInstanceOf(TickerNotFoundException.class);
    }
}