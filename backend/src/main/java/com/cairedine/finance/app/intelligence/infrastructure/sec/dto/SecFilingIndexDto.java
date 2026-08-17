package com.cairedine.finance.app.intelligence.infrastructure.sec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO pour le fichier d'index {accessionNumber}-index.json de la SEC.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecFilingIndexDto(
        @JsonProperty("directory") SecDirectoryDto directory
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecDirectoryDto(
            @JsonProperty("item") List<SecFilingDocDto> item,
            @JsonProperty("name") String name
    ) {}
}
