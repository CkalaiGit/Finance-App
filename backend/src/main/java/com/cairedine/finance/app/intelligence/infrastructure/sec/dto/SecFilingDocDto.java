package com.cairedine.finance.app.intelligence.infrastructure.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente un fichier individuel listé dans l'index d'un filing SEC.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecFilingDocDto(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("size") String size,
        @JsonProperty("description") String description
) {
}
