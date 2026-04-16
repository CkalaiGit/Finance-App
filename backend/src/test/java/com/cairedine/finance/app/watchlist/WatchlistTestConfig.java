package com.cairedine.finance.app.watchlist;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Configuration de test pour le module Watchlist.
 * Fournit un JwtDecoder mock pour éviter les appels réseau en test.
 */
@TestConfiguration
public class WatchlistTestConfig {

    /**
     * Fournit un JwtDecoder mock pour les tests.
     * Cela évite l'erreur "No qualifying bean of type 'JwtDecoder' available".
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }
}

