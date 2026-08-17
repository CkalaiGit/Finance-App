package com.cairedine.finance.app.intelligence.infrastructure.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Conteneur des soumissions récentes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecFilingsDto(
        @JsonProperty("recent") SecRecentFilingsDto recent
) {
}
