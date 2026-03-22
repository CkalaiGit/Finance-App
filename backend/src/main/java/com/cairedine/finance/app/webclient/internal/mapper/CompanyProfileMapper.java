package com.cairedine.finance.app.webclient.internal.mapper;

import com.cairedine.finance.app.webclient.internal.dto.FmpCompanyProfileDto;
import com.cairedine.finance.app.webclient.CompanyProfileRecord;
import org.springframework.stereotype.Component;

@Component
public class CompanyProfileMapper extends BaseMapper {

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
}
