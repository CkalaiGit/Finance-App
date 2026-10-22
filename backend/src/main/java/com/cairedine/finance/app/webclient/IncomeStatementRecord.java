package com.cairedine.finance.app.webclient;

import java.math.BigDecimal;

public record IncomeStatementRecord(
        String symbol,
        String date,
        BigDecimal revenue,
        BigDecimal operatingIncome,
        BigDecimal ebitda,
        BigDecimal eps,
        BigDecimal sellingGeneralAndAdministrativeExpenses
) {}
