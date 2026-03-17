package com.cairedine.finance.app.webclient;

import com.cairedine.finance.app.webclient.record.CompanyProfileRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
}