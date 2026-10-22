package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpKeyMetricsTtmDto(
        String symbol,
        Double returnOnInvestedCapitalTTM,
        @JsonProperty("netDebtToEBITDATTM") Double netDebtToEBITDATTM,
        @JsonProperty("enterpriseValueTTM") Double enterpriseValueTTM,
        @JsonProperty("priceToEarningsRatioTTM") Double peRatioTTM,
        @JsonProperty("evToSalesTTM") Double evToSalesTTM
) {}
