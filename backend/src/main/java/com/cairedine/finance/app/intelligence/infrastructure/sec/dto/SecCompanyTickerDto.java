package com.cairedine.finance.app.intelligence.infrastructure.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente une entrée du fichier officiel <a href="https://www.sec.gov/files/company_tickers.json">...</a>.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecCompanyTickerDto(
        @JsonProperty("cik_str") Long cikStr,
        @JsonProperty("ticker") String ticker,
        @JsonProperty("title") String title
) {
    public String formattedCik() {
        return cikStr != null ? String.format("%010d", cikStr) : null;
    }
}
