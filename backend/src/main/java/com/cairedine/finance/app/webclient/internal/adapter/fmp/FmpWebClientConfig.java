package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
class FmpWebClientConfig {

    private final FmpProperties fmpProperties;

    @Bean
    RestClient fmpRestClient() {
        return RestClient.builder()
                .baseUrl(fmpProperties.baseUrl())
                .defaultHeader("apikey", fmpProperties.apiKey())
                .build();
    }
}
