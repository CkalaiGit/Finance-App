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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitaire du {@link GlobalExceptionHandler}.
 * Vérifie que le handler traduit les exceptions en {@code ProblemDetail} avec le bon
 * code HTTP.
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
    @DisplayName("TickerNotFoundException → 404 Not Found avec ProblemDetail")
    void tickerNotFoundException_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/ticker-not-found")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Ticker not found : INVALID"))
                .andExpect(jsonPath("$.code").value("TICKER_NOT_FOUND"));
    }

    @Test
    @DisplayName("MarketDataUnavailableException → 503 Service Unavailable avec ProblemDetail")
    void marketDataUnavailableException_shouldReturn503() throws Exception {
        mockMvc.perform(get("/test/market-data-unavailable")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Market data unavailable for : AAPL"))
                .andExpect(jsonPath("$.code").value("MARKET_DATA_UNAVAILABLE"));
    }

    @Test
    @DisplayName("TickerAlreadyInWatchlistException → 409 Conflict avec ProblemDetail")
    void tickerAlreadyInWatchlistException_shouldReturn409() throws Exception {
        mockMvc.perform(get("/test/ticker-already-in-watchlist")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Le ticker AAPL existe déjà dans la watchlist"))
                .andExpect(jsonPath("$.code").value("TICKER_ALREADY_IN_WATCHLIST"));
    }

    @Test
    @DisplayName("TickerNotInWatchlistException → 404 Not Found avec ProblemDetail")
    void tickerNotInWatchlistException_shouldReturn404() throws Exception {
        mockMvc.perform(get("/test/ticker-not-in-watchlist")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Le ticker MSFT n'existe pas dans la watchlist"))
                .andExpect(jsonPath("$.code").value("TICKER_NOT_IN_WATCHLIST"));
    }

    @Test
    @DisplayName("InvalidTickerException → 400 Bad Request avec ProblemDetail")
    void invalidTickerException_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/invalid-ticker")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Le ticker ne peut pas être vide"))
                .andExpect(jsonPath("$.code").value("INVALID_TICKER"));
    }

    @Test
    @DisplayName("WatchlistException → 400 avec code WATCHLIST_ERROR")
    void watchlistException_shouldReturn400WithCode() throws Exception {
        mockMvc.perform(get("/test/watchlist-error")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WATCHLIST_ERROR"));
    }

    @Test
    @DisplayName("AccessDeniedException → 403 avec code ACCESS_DENIED")
    void accessDeniedException_shouldReturn403WithCode() throws Exception {
        mockMvc.perform(get("/test/access-denied")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("AuthenticationException → 401 avec code UNAUTHENTICATED")
    void authenticationException_shouldReturn401WithCode() throws Exception {
        mockMvc.perform(get("/test/unauthenticated")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Corps invalide → 400 avec erreurs de validation")
    void invalidRequestBody_shouldReturn400WithValidationErrors() throws Exception {
        mockMvc.perform(post("/test/invalid-body")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 avec code VALIDATION_ERROR")
    void constraintViolationException_shouldReturn400WithValidationCode() throws Exception {
        mockMvc.perform(get("/test/constraint-violation")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Unexpected RuntimeException → 500 sans fuite du message interne")
    void unexpectedException_shouldReturn500() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Une erreur interne s'est produite"))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                // S'assurer que le message interne n'est pas exposé
                .andExpect(jsonPath("$.detail").value("Une erreur interne s'est produite"));
    }

    @Test
    @DisplayName("IllegalStateException → 500 (pas 400)")
    void illegalStateException_shouldReturn500() throws Exception {
        mockMvc.perform(get("/test/illegal-state-exception")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }
}
