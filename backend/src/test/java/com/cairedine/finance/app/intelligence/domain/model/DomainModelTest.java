package com.cairedine.finance.app.intelligence.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    @DisplayName("Devrait instancier correctement l'enum InsightType et ses instructions")
    void testInsightType() {
        assertThat(InsightType.EARNINGS_SUMMARY.getPromptInstruction()).contains("résultats financiers");
        assertThat(InsightType.RISK_FACTORS.getPromptInstruction()).contains("facteurs de risque");
        assertThat(InsightType.CAPITAL_ALLOCATION.getPromptInstruction()).contains("allocation du capital");
    }

    @Test
    @DisplayName("Devrait créer un CompanyDocument à partir d'un SecFiling via la méthode factory")
    void testCompanyDocumentFactory() {
        SecFiling filing = new SecFiling("000123-24-001", "10-K", "2024-12-31", "<html/>", Instant.now());
        CompanyDocument doc = CompanyDocument.from(filing, "AAPL", 12);

        assertThat(doc.ticker()).isEqualTo("AAPL");
        assertThat(doc.source()).isEqualTo("SEC_EDGAR");
        assertThat(doc.accessionNumber()).isEqualTo("000123-24-001");
        assertThat(doc.formType()).isEqualTo("10-K");
        assertThat(doc.chunkCount()).isEqualTo(12);
        assertThat(doc.id()).isNotNull();
    }

    @Test
    @DisplayName("Devrait vérifier la présence de sources dans CompanyInsight")
    void testCompanyInsightHasDocuments() {
        CompanyInsight insightWithSources = new CompanyInsight(
                "AAPL",
                InsightType.EARNINGS_SUMMARY,
                "Résumé",
                List.of("Point 1"),
                List.of("Risque 1"),
                "Perspectives",
                List.of("000123-24-001"),
                Instant.now()
        );

        CompanyInsight insightWithoutSources = new CompanyInsight(
                "AAPL",
                InsightType.EARNINGS_SUMMARY,
                "Résumé",
                List.of(),
                List.of(),
                "Perspectives",
                List.of(),
                Instant.now()
        );

        assertThat(insightWithSources.hasDocuments()).isTrue();
        assertThat(insightWithoutSources.hasDocuments()).isFalse();
    }
}
