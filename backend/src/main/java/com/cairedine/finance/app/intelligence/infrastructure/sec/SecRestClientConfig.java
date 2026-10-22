package com.cairedine.finance.app.intelligence.infrastructure.sec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration des clients HTTP RestClient dédiés à SEC EDGAR.
 * Utilise java.net.http.HttpClient (JdkClientHttpRequestFactory) optimisé pour Java 25 et les Virtual Threads.
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
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }
}
