package com.cairedine.finance.app.webclient.internal.mapper;

import java.math.BigDecimal;

public abstract class BaseMapper {

    protected BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}

