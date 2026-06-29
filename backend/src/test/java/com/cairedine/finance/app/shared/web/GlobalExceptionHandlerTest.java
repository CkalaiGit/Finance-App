package com.cairedine.finance.app.shared.web;

import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.security.SecurityConfig;
import com.cairedine.finance.app.user.infrastructure.security.WebConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitaire du {@link GlobalExceptionHandler}.
 * et vérifie que le handler les traduit en {@code ProblemDetail} avec le bon
 * code HTTP.</p>
 *
 * <p>La sécurité est exclue car ce test ne porte que sur le mapping
 * exception → HTTP status, pas sur l'authentification.</p>
 */
@WebMvcTest(controllers = StubExceptionController.class)
@Import({SecurityConfig.class, WebConfig.class})
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserSyncService userSyncService;

    @Test
    @DisplayName("WatchlistException → 400 Bad Request avec ProblemDetail")
    void watchlistException_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/watchlist-exception")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Watchlist already exists"));
    }

    @Test
    @DisplayName("TickerNotFoundException → 404 Not Found avec ProblemDetail")
    void tickerNotFoundException_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/ticker-not-found")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Ticker not found : INVALID"));
    }

    @Test
    @DisplayName("MarketDataUnavailableException → 503 Service Unavailable avec ProblemDetail")
    void marketDataUnavailableException_shouldReturn503() throws Exception {
        mockMvc.perform(get("/test/market-data-unavailable")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Market data unavailable for : AAPL"));
    }
}
