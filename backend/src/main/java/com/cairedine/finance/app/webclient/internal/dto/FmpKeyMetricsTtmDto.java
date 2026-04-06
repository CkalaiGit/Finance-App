package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpKeyMetricsTtmDto(
        String symbol,
        Double returnOnInvestedCapitalTTM,
        Double netDebtToEBITDATTM,
        Double evToOperatingCashFlowTTM,
        Double peRatioTTM,
        Double evToSalesTTM
) {}
