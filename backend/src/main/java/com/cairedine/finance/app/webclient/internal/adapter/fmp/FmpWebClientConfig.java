package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FmpProperties.class)
class FmpWebClientConfig {

    private final FmpProperties fmpProperties;

    @Bean
    RestClient fmpRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(fmpProperties.baseUrl())
                .defaultHeader("apikey", fmpProperties.apiKey())
                .requestFactory(requestFactory)
                .build();
    }
}

