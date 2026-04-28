package com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper;

import com.cairedine.finance.app.webclient.CompanyProfileRecord;
import com.cairedine.finance.app.webclient.web.dto.CompanyProfileResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir CompanyProfileRecord (domaine) en CompanyProfileResponse (DTO HTTP).
 * Suit le pattern adapter pour l'exposition des données au frontend.
 */
@Component
public class CompanyProfileWebMapper {

    public CompanyProfileResponse toResponse(CompanyProfileRecord record) {
        if (record == null) {
            return null;
        }

        return new CompanyProfileResponse(
                record.symbol(),
                record.companyName(),
                record.industry(),
                record.description(),
                record.image(),
                record.price(),
                record.beta(),
                record.marketCap()
        );
    }
}

