package com.cairedine.finance.app.intelligence.domain.model;

/**
 * Sections d'intérêt extraites des filings SEC.
 */
public enum InsightSection {
    RISK_FACTORS("Risk Factors", "Summarize the principal risks and potential impact on profitability."),
    MANAGEMENT_DISCUSSION("Management Discussion", "Summarize management commentary on operations and financial results."),
    GUIDANCE("Guidance", "Extract forward-looking statements and projections from management."),
    SUPPLY_CHAIN("Supply Chain", "Identify supply chain, suppliers, and logistics mentions."),
    KEY_HIGHLIGHTS("Key Highlights", "List 3-5 key facts or highlights from the filing.");

    private final String sectionTitle;
    private final String summaryInstruction;

    InsightSection(String sectionTitle, String summaryInstruction) {
        this.sectionTitle = sectionTitle;
        this.summaryInstruction = summaryInstruction;
    }

    public String sectionTitle() {
        return sectionTitle;
    }

    public String summaryInstruction() {
        return summaryInstruction;
    }
}
