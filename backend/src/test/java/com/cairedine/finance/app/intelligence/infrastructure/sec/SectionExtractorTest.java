package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.model.InsightSection;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SectionExtractorTest {

    private final HtmlTextCleaner cleaner = new HtmlTextCleaner();

    @Test
    void shouldIgnoreTableOfContentsAndExtractBodySections() {
        SectionExtractor extractor = new SectionExtractor(80_000);

        List<SectionContent> sections = extractor.extractSections(cleaner.clean(fakeTenKHtml()));

        assertThat(sections)
                .extracting(SectionContent::section)
                .containsExactly(InsightSection.RISK_FACTORS, InsightSection.MANAGEMENT_DISCUSSION);
        assertThat(section(sections, InsightSection.RISK_FACTORS).rawText())
                .contains("Risk factors affecting our business");
        assertThat(section(sections, InsightSection.MANAGEMENT_DISCUSSION).rawText())
                .contains("Management discussion of operating results");
        assertThat(section(sections, InsightSection.RISK_FACTORS).wordCount())
                .isEqualTo(section(sections, InsightSection.RISK_FACTORS).rawText().trim().split("\\s+").length);
    }

    @Test
    void shouldReturnKeyHighlightsWhenNoQualitativeSectionIsFound() {
        SectionExtractor extractor = new SectionExtractor(80_000);

        List<SectionContent> sections = extractor.extractSections("General filing text without section headings.");

        assertThat(sections).singleElement()
                .extracting(SectionContent::section)
                .isEqualTo(InsightSection.KEY_HIGHLIGHTS);
    }

    @Test
    void shouldReturnNoSectionsForEmptyText() {
        assertThat(new SectionExtractor(80_000).extractSections(" \n ")).isEmpty();
    }

    @Test
    void shouldTruncateSectionsToConfiguredTokenLimit() {
        SectionExtractor extractor = new SectionExtractor(30);

        List<SectionContent> sections = extractor.extractSections(cleaner.clean(fakeTenKHtml()));

        assertThat(sections.stream()
                .mapToInt(section -> extractor.estimateTokens(section.rawText()))
                .sum()).isLessThanOrEqualTo(30);
        assertThat(sections).allSatisfy(section -> assertThat(section.wordCount())
                .isEqualTo(section.rawText().isBlank() ? 0 : section.rawText().trim().split("\\s+").length));
    }

    private SectionContent section(List<SectionContent> sections, InsightSection section) {
        return sections.stream()
                .filter(content -> content.section() == section)
                .findFirst()
                .orElseThrow();
    }

    private String fakeTenKHtml() {
        String riskText = "Risk factors affecting our business include market volatility and competition. "
                .repeat(35);
        String managementText = "Management discussion of operating results describes revenue growth and costs. "
                .repeat(35);

        return """
                <html><body>
                  <h1>TABLE OF CONTENTS</h1>
                  <p>ITEM 1A. RISK FACTORS 3</p>
                  <p>ITEM 7. MANAGEMENT'S DISCUSSION AND ANALYSIS 25</p>
                  <h2>ITEM 1A. RISK FACTORS</h2>
                  <p>%s</p>
                  <p><ix:nonfraction name="us-gaap:Revenue">72.5</ix:nonfraction></p>
                  <h2>ITEM 1B. UNRESOLVED STAFF COMMENTS</h2>
                  <h2>ITEM 7. MANAGEMENT'S DISCUSSION AND ANALYSIS</h2>
                  <p>%s</p>
                  <h2>ITEM 8. FINANCIAL STATEMENTS</h2>
                </body></html>
                """.formatted(riskText, managementText);
    }
}
