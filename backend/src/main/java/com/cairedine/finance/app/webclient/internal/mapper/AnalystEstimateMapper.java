package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.AnalystEstimateRecord;
import com.cairedine.finance.app.webclient.internal.dto.FmpAnalystEstimateDto;
import org.springframework.stereotype.Component;

@Component
public class AnalystEstimateMapper extends BaseMapper {

    public AnalystEstimateRecord toRecord(FmpAnalystEstimateDto dto) {
        return new AnalystEstimateRecord(
                dto.symbol(),
                dto.date(),
                toBigDecimal(dto.epsAvg()),
                toBigDecimal(dto.revenueAvg())
        );
    }
}