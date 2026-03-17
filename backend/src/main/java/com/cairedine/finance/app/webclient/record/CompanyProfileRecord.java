package com.cairedine.finance.app.webclient.record;

import java.math.BigDecimal;

public record CompanyProfileRecord(
        String symbol,
        String companyName,
        String industry,
        String image,
        BigDecimal beta,
        BigDecimal marketCap
) {}
