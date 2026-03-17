package com.cairedine.finance.app.webclient.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FmpCompanyProfileDto(
        String symbol,
        String companyName,
        String industry,
        String image,
        Double beta,
        Double marketCap
) {}
