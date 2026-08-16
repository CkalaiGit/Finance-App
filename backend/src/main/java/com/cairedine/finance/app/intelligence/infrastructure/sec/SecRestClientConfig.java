package com.cairedine.finance.app.intelligence.infrastructure.sec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration des clients HTTP RestClient dédiés à SEC EDGAR.
 */
@Configuration
public class SecRestClientConfig {

    @Bean
    public RestClient secDataRestClient(
            @Value("${intelligence.sec.base-url:https://data.sec.gov}") String baseUrl,
            @Value("${intelligence.sec.user-agent:cairedine.finance contact@cairedine.com}") String userAgent) {
        return buildRestClient(baseUrl, userAgent);
    }

    @Bean
    public RestClient secWwwRestClient(
            @Value("${intelligence.sec.www-url:https://www.sec.gov}") String baseUrl,
            @Value("${intelligence.sec.user-agent:cairedine.finance contact@cairedine.com}") String userAgent) {
        return buildRestClient(baseUrl, userAgent);
    }

    private RestClient buildRestClient(String baseUrl, String userAgent) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                .build();
    }
}
