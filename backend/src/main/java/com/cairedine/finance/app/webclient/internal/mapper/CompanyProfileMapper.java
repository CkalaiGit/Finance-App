package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.internal.dto.FmpCompanyProfileDto;
import com.cairedine.finance.app.webclient.CompanyProfileRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CompanyProfileMapper {

    public CompanyProfileRecord toRecord(FmpCompanyProfileDto dto) {
        return new CompanyProfileRecord(
                dto.symbol(),
                dto.companyName(),
                dto.industry(),
                dto.image(),
                toBigDecimal(dto.beta()),
                toBigDecimal(dto.marketCap())
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}
