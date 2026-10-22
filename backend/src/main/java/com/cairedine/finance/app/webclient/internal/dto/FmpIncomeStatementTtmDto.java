package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpIncomeStatementTtmDto(
        String symbol,
        String date,
        Double revenue,
        Double operatingIncome,
        Double ebitda,
        Double eps,
        Double sellingGeneralAndAdministrativeExpenses
) {}
