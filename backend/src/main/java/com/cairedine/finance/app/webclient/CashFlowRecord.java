package com.cairedine.finance.app.webclient;

import java.math.BigDecimal;

public record CashFlowRecord(
        String symbol,
        BigDecimal freeCashFlow,
        BigDecimal commonStockRepurchased
) {}

