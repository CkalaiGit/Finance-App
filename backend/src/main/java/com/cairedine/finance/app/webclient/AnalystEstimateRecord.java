package com.cairedine.finance.app.webclient;

import java.math.BigDecimal;

public record AnalystEstimateRecord(
        String symbol,
        String date,
        BigDecimal epsAvg,
        BigDecimal revenueAvg
) {}