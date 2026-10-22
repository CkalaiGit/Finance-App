package com.cairedine.finance.app.intelligence.infrastructure.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Tableaux parallèles retournés par l'API SEC EDGAR (/submissions/CIK{cik}.json -> filings.recent).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecRecentFilingsDto(
        @JsonProperty("accessionNumber") List<String> accessionNumber,
        @JsonProperty("filingDate") List<String> filingDate,
        @JsonProperty("reportDate") List<String> reportDate,
        @JsonProperty("form") List<String> form,
        @JsonProperty("primaryDocument") List<String> primaryDocument,
        @JsonProperty("primaryDocDescription") List<String> primaryDocDescription
) {
    public int size() {
        return form != null ? form.size() : 0;
    }
}
