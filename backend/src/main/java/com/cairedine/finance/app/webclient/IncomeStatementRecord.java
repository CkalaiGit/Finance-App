package com.cairedine.finance.app.webclient;

import java.math.BigDecimal;

public record IncomeStatementRecord(
        String symbol,
        BigDecimal revenue,
        BigDecimal operatingIncome,
        BigDecimal eps,
        BigDecimal sellingGeneralAndAdministrativeExpenses
) {}
