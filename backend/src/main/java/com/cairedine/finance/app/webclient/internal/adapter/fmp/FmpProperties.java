package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fmp")
public record FmpProperties(String apiKey, String baseUrl) {}
