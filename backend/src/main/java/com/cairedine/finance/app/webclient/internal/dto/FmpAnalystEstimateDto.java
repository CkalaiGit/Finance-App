package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpAnalystEstimateDto(
        String symbol,
        String date,
        Double epsAvg,
        Double revenueAvg
) {}

