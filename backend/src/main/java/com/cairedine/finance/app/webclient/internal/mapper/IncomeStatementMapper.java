package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.IncomeStatementRecord;
import com.cairedine.finance.app.webclient.internal.dto.FmpIncomeStatementTtmDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class IncomeStatementMapper {

    public IncomeStatementRecord toRecord(FmpIncomeStatementTtmDto dto) {
        return new IncomeStatementRecord(
                dto.symbol(),
                toBigDecimal(dto.revenue()),
                toBigDecimal(dto.operatingIncome()),
                toBigDecimal(dto.eps()),
                toBigDecimal(dto.sellingGeneralAndAdministrativeExpenses())
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}
