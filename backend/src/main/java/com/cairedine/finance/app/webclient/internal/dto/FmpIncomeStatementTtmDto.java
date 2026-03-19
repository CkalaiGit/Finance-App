package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpIncomeStatementTtmDto(
        String symbol,
        Double revenue,
        Double operatingIncome,
        Double eps,
        Double sellingGeneralAndAdministrativeExpenses
) {}