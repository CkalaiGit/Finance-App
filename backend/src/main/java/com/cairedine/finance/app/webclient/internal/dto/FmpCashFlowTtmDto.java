package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpCashFlowTtmDto(
        String symbol,
        String date,
        Double freeCashFlow,
        Double commonStockRepurchased
) {}
