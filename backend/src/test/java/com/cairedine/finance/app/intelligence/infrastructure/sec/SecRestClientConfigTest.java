package com.cairedine.finance.app.intelligence.infrastructure.sec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class SecRestClientConfigTest {

    private final SecRestClientConfig config = new SecRestClientConfig();

    @Test
    @DisplayName("Configuration des beans RestClient pour SEC Data et SEC Www")
    void shouldCreateRestClientBeans() {
        RestClient secDataClient = config.secDataRestClient("https://data.sec.gov", "TestApp contact@test.com");
        RestClient secWwwClient = config.secWwwRestClient("https://www.sec.gov", "TestApp contact@test.com");

        assertThat(secDataClient).isNotNull();
        assertThat(secWwwClient).isNotNull();
    }
}
