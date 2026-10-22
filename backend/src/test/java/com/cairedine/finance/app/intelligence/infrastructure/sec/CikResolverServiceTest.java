package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CikResolverServiceTest {

    private MockRestServiceServer mockServer;
    private CikResolverService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://www.sec.gov");
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        this.service = new CikResolverService(restClient);
    }

    @Test
    @DisplayName("Chargement réussi du mapping SEC et résolution d'un ticker en CIK 10 chiffres")
    void shouldSuccessfullyLoadMappingsAndResolveTicker() {
        String jsonPayload = """
                {
                  "0": { "cik_str": 320193, "ticker": "AAPL", "title": "Apple Inc." },
                  "1": { "cik_str": 1045810, "ticker": "NVDA", "title": "NVIDIA CORP" },
                  "2": { "cik_str": 789019, "ticker": "MSFT", "title": "MICROSOFT CORP" }
                }
                """;

        mockServer.expect(requestTo("https://www.sec.gov/files/company_tickers.json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        service.loadMappings();
        mockServer.verify();

        assertThat(service.isKnown("AAPL")).isTrue();
        assertThat(service.isKnown("nvda")).isTrue();
        assertThat(service.isKnown("UNKNOWN")).isFalse();

        assertThat(service.resolve("AAPL")).isEqualTo("0000320193");
        assertThat(service.resolve("aapl")).isEqualTo("0000320193");
        assertThat(service.resolve("NVDA")).isEqualTo("0001045810");
        assertThat(service.resolve("MSFT")).isEqualTo("0000789019");
    }

    @Test
    @DisplayName("Lève TickerNotFoundException lorsque le ticker est inconnu")
    void shouldThrowTickerNotFoundExceptionWhenTickerNotFound() {
        String jsonPayload = """
                {
                  "0": { "cik_str": 320193, "ticker": "AAPL", "title": "Apple Inc." }
                }
                """;

        mockServer.expect(requestTo("https://www.sec.gov/files/company_tickers.json"))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        service.loadMappings();

        assertThrows(TickerNotFoundException.class, () -> service.resolve("UNKNOWN"));
        assertThrows(TickerNotFoundException.class, () -> service.resolve(null));
        assertThrows(TickerNotFoundException.class, () -> service.resolve("   "));
    }

    @Test
    @DisplayName("Gère gracieusement une erreur réseau au démarrage sans crasher")
    void shouldHandleNetworkFailureGracefullyAtStartup() {
        mockServer.expect(requestTo("https://www.sec.gov/files/company_tickers.json"))
                .andRespond(withServerError());

        // Doit catcher l'erreur et ne pas lever d'exception
        service.loadMappings();
        mockServer.verify();

        assertThat(service.isKnown("AAPL")).isFalse();
        assertThrows(TickerNotFoundException.class, () -> service.resolve("AAPL"));
    }
}
