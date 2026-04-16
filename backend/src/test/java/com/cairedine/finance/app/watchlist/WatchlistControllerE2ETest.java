package com.cairedine.finance.app.watchlist;

import com.cairedine.finance.app.watchlist.infrastructure.persistence.entity.WatchlistItemJpaEntity;
import com.cairedine.finance.app.watchlist.infrastructure.persistence.repository.IWatchlistJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WatchlistController Integration Tests")
class WatchlistControllerE2ETest {

    private static final String KEYCLOAK_ID = "user-123";
    private static final String TICKER_AAPL = "AAPL";
    private static final String TICKER_MSFT = "MSFT";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IWatchlistJpaRepository watchlistRepository;

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
    }

    @Test
    @DisplayName("Devrait ajouter un ticker à la watchlist")
    void shouldAddTickerToWatchlist() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist/{ticker}", TICKER_AAPL)
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.keycloakId").value(KEYCLOAK_ID))
            .andExpect(jsonPath("$.tickers", hasItem(TICKER_AAPL)));

        // Vérifier que le ticker a été sauvegardé en base
        assert watchlistRepository.existsByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_AAPL);
    }

    @Test
    @DisplayName("Devrait retourner 400 si le ticker existe déjà")
    void shouldReturn400IfTickerAlreadyExists() throws Exception {
        // Arrange : Ajouter le ticker d'abord
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        watchlistRepository.save(item);

        // Act & Assert : Essayer d'ajouter le même ticker
        mockMvc.perform(post("/api/v1/watchlist/{ticker}", TICKER_AAPL)
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait retourner 401 si non authentifié")
    void shouldReturn401IfNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist/{ticker}", TICKER_AAPL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait supprimer un ticker de la watchlist")
    void shouldRemoveTickerFromWatchlist() throws Exception {
        // Arrange : Ajouter le ticker d'abord
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        watchlistRepository.save(item);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/watchlist/{ticker}", TICKER_AAPL)
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keycloakId").value(KEYCLOAK_ID))
            .andExpect(jsonPath("$.tickers", not(hasItem(TICKER_AAPL))));

        // Vérifier que le ticker a été supprimé de la base
        assert !watchlistRepository.existsByKeycloakIdAndTicker(KEYCLOAK_ID, TICKER_AAPL);
    }

    @Test
    @DisplayName("Devrait retourner 400 si le ticker à supprimer n'existe pas")
    void shouldReturn400IfTickerToDeleteNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/watchlist/{ticker}", TICKER_AAPL)
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Devrait récupérer la watchlist complète")
    void shouldGetCompleteWatchlist() throws Exception {
        // Arrange : Ajouter plusieurs tickers
        WatchlistItemJpaEntity item1 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        WatchlistItemJpaEntity item2 = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_MSFT)
            .build();
        watchlistRepository.saveAll(java.util.List.of(item1, item2));

        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keycloakId").value(KEYCLOAK_ID))
            .andExpect(jsonPath("$.tickers", hasSize(2)))
            .andExpect(jsonPath("$.tickers", hasItems(TICKER_AAPL, TICKER_MSFT)));
    }

    @Test
    @DisplayName("Devrait retourner une watchlist vide si l'utilisateur n'a aucun ticker")
    void shouldReturnEmptyWatchlistIfNoTickers() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keycloakId").value(KEYCLOAK_ID))
            .andExpect(jsonPath("$.tickers", hasSize(0)));
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un ticker dans la watchlist")
    void shouldCheckTickerExists() throws Exception {
        // Arrange : Ajouter le ticker
        WatchlistItemJpaEntity item = WatchlistItemJpaEntity.builder()
            .keycloakId(KEYCLOAK_ID)
            .ticker(TICKER_AAPL)
            .build();
        watchlistRepository.save(item);

        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist/{ticker}/exists", TICKER_AAPL)
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value(TICKER_AAPL))
            .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @DisplayName("Devrait retourner false si le ticker n'existe pas")
    void shouldReturnFalseIfTickerNotExists() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist/{ticker}/exists", TICKER_AAPL)
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value(TICKER_AAPL))
            .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    @DisplayName("Devrait convertir les tickers en majuscules")
    void shouldConvertTickersToUpperCase() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist/{ticker}", "aapl")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(KEYCLOAK_ID))
                )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tickers", hasItem("AAPL")));

        // Vérifier que le ticker est stocké en majuscules
        assert watchlistRepository.existsByKeycloakIdAndTicker(KEYCLOAK_ID, "AAPL");
    }
}

