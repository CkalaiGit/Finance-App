package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.internal.dto.FmpKeyMetricsTtmDto;
import org.springframework.stereotype.Component;
import com.cairedine.finance.app.webclient.KeyMetricsRecord;
@Component
public class KeyMetricsMapper extends BaseMapper {

    public KeyMetricsRecord toRecord(FmpKeyMetricsTtmDto dto) {
        return new KeyMetricsRecord(
                dto.symbol(),
                toBigDecimal(dto.returnOnInvestedCapitalTTM()),
                toBigDecimal(dto.netDebtToEBITDATTM()),
                toBigDecimal(dto.enterpriseValueTTM()),
                toBigDecimal(dto.peRatioTTM()),
                toBigDecimal(dto.evToSalesTTM())
        );
    }
}
