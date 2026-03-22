package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.CashFlowRecord;
import com.cairedine.finance.app.webclient.internal.dto.FmpCashFlowTtmDto;
import org.springframework.stereotype.Component;

@Component
public class CashFlowMapper extends BaseMapper {

    public CashFlowRecord toRecord(FmpCashFlowTtmDto dto) {
        return new CashFlowRecord(
                dto.symbol(),
                toBigDecimal(dto.freeCashFlow()),
                toBigDecimal(dto.commonStockRepurchased())
        );
    }
}

