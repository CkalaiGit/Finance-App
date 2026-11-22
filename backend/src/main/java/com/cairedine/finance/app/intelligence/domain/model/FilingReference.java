package com.cairedine.finance.app.intelligence.domain.model;

public record FilingReference(
        String accessionNumber,
        String formType,
        String filingDate,
        String primaryDocument
) {
}
