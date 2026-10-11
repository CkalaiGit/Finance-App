package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import com.cairedine.finance.app.shared.exceptions.MarketDataUnavailableException;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import com.cairedine.finance.app.webclient.CompanyProfileRecord;
import com.cairedine.finance.app.webclient.internal.mapper.AnalystEstimateMapper;
import com.cairedine.finance.app.webclient.internal.mapper.CashFlowMapper;
import com.cairedine.finance.app.webclient.internal.mapper.CompanyProfileMapper;
import com.cairedine.finance.app.webclient.internal.mapper.IncomeStatementMapper;
import com.cairedine.finance.app.webclient.internal.mapper.KeyMetricsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FmpMarketDataAdapterTest {

    private static final String SYMBOL = "AAPL";
    private static final String PROFILE_URL = "https://fmp.test/stable/profile?symbol=AAPL";

    private MockRestServiceServer server;
    private FmpMarketDataAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fmp.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new FmpMarketDataAdapter(
                builder.build(),
                new CompanyProfileMapper(),
                new IncomeStatementMapper(),
                new CashFlowMapper(),
                new AnalystEstimateMapper(),
                new KeyMetricsMapper());
    }

    @Test
    void shouldTranslateServerErrorToMarketDataUnavailable() {
        server.expect(requestTo(PROFILE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        MarketDataUnavailableException exception = assertThrows(
                MarketDataUnavailableException.class,
                () -> adapter.fetchCompanyProfile(SYMBOL));
        assertNotNull(exception.getCause());
        server.verify();
    }

    @Test
    void shouldTranslateRateLimitToMarketDataUnavailable() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(MarketDataUnavailableException.class, () -> adapter.fetchCompanyProfile(SYMBOL));
        server.verify();
    }

    @Test
    void shouldTranslateInvalidApiKeyToMarketDataUnavailable() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(MarketDataUnavailableException.class, () -> adapter.fetchCompanyProfile(SYMBOL));
        server.verify();
    }

    @Test
    void shouldTranslateNotFoundToTickerNotFound() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(TickerNotFoundException.class, () -> adapter.fetchCompanyProfile(SYMBOL));
        server.verify();
    }

    @Test
    void shouldTranslateTimeoutToMarketDataUnavailable() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withException(new SocketTimeoutException("Request timed out")));

        assertThrows(MarketDataUnavailableException.class, () -> adapter.fetchCompanyProfile(SYMBOL));
        server.verify();
    }

    @Test
    void shouldTranslateInvalidJsonToMarketDataUnavailable() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("{invalid json", MediaType.APPLICATION_JSON));

        assertThrows(MarketDataUnavailableException.class, () -> adapter.fetchCompanyProfile(SYMBOL));
        server.verify();
    }

    @Test
    void shouldReturnEmptyForEmptyProfileAndIncomeResponses() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://fmp.test/stable/income-statement?symbol=AAPL&limit=4"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Optional<CompanyProfileRecord> profileResult = adapter.fetchCompanyProfile(SYMBOL);

        assertEquals(Optional.empty(), profileResult);
        assertEquals(List.of(), adapter.fetchIncomeStatements(SYMBOL, 4));
        server.verify();
    }

    @Test
    void shouldMapNominalProfileResponse() {
        server.expect(requestTo(PROFILE_URL))
                .andRespond(withSuccess("""
                        [{
                          "symbol": "AAPL",
                          "companyName": "Apple Inc.",
                          "industry": "Consumer Electronics",
                          "image": "https://example.test/apple.png",
                          "beta": 1.2,
                          "marketCap": 3000000,
                          "description": "Technology company",
                          "price": 200.5
                        }]
                        """, MediaType.APPLICATION_JSON));

        Optional<CompanyProfileRecord> result = adapter.fetchCompanyProfile(SYMBOL);

        assertEquals(Optional.of(new CompanyProfileRecord(
                SYMBOL,
                "Apple Inc.",
                "Consumer Electronics",
                "https://example.test/apple.png",
                new BigDecimal("1.2"),
                new BigDecimal("3000000.0"),
                "Technology company",
                new BigDecimal("200.5"))), result);
        server.verify();
    }
}
