package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.IncomeStatementRecord;
import com.cairedine.finance.app.webclient.internal.dto.FmpIncomeStatementTtmDto;
import org.springframework.stereotype.Component;

@Component
public class IncomeStatementMapper extends BaseMapper{

    public IncomeStatementRecord toRecord(FmpIncomeStatementTtmDto dto) {
        return new IncomeStatementRecord(
                dto.symbol(),
                toBigDecimal(dto.revenue()),
                toBigDecimal(dto.operatingIncome()),
                toBigDecimal(dto.eps()),
                toBigDecimal(dto.sellingGeneralAndAdministrativeExpenses())
        );
    }
}
