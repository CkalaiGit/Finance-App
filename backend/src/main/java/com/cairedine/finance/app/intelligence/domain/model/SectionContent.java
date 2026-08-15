package com.cairedine.finance.app.intelligence.domain.model;

/**
 * Contenu d'une section extraite d'un filing SEC.
 */
public record SectionContent(InsightSection section, String rawText, int wordCount) {

    public boolean isEmpty() {
        return rawText == null || rawText.isBlank();
    }

}
