package com.cairedine.finance.app.webclient.record;

import java.math.BigDecimal;

public record CompanyProfileRecord(
        String symbol,
        BigDecimal beta,
        BigDecimal marketCap
) {}
