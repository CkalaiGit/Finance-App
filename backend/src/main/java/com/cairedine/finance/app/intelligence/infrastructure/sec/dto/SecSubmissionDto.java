package com.cairedine.finance.app.intelligence.infrastructure.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Représente la réponse principale de l'API SEC EDGAR (/submissions/CIK{cik}.json).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecSubmissionDto(
        @JsonProperty("cik") String cik,
        @JsonProperty("entityType") String entityType,
        @JsonProperty("name") String name,
        @JsonProperty("tickers") List<String> tickers,
        @JsonProperty("filings") SecFilingsDto filings
) {
}
