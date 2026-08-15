package com.cairedine.finance.app.intelligence.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    @DisplayName("Devrait instancier correctement l'enum InsightSection et ses propriétés")
    void testInsightSection() {
        assertThat(InsightSection.RISK_FACTORS.sectionTitle()).isEqualTo("Risk Factors");
        assertThat(InsightSection.MANAGEMENT_DISCUSSION.sectionTitle()).isEqualTo("Management Discussion");
        assertThat(InsightSection.GUIDANCE.sectionTitle()).isEqualTo("Guidance");
        assertThat(InsightSection.RISK_FACTORS.summaryInstruction()).contains("principal risks");
    }

    @Test
    @DisplayName("Devrait vérifier SectionContent et méthode isEmpty")
    void testSectionContentIsEmpty() {
        SectionContent emptyContent = new SectionContent(InsightSection.RISK_FACTORS, "", 0);
        SectionContent nullContent = new SectionContent(InsightSection.RISK_FACTORS, null, 0);
        SectionContent validContent = new SectionContent(InsightSection.RISK_FACTORS, "Text content", 2);

        assertThat(emptyContent.isEmpty()).isTrue();
        assertThat(nullContent.isEmpty()).isTrue();
        assertThat(validContent.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Devrait tester CompanyInsight et hasSupplyChainData")
    void testCompanyInsight() {
        CompanyInsight insightWithSupplyChain = new CompanyInsight(
                "AAPL",
                "10-K",
                "2024",
                "0000320193-24-000106",
                "Excellente performance globale...",
                "Marges opérationnelles en hausse à 30.5%...",
                List.of("Risque de change", "Pression réglementaire"),
                "Guidance positive pour Q1 2025",
                "Diversification des fournisseurs en cours",
                List.of("Lancement Apple Vision Pro"),
                Instant.now()
        );

        CompanyInsight insightWithoutSupplyChain = new CompanyInsight(
                "AAPL",
                "10-K",
                "2024",
                "0000320193-24-000106",
                "Performance...",
                "Marges...",
                List.of("Risque"),
                "Guidance",
                null,
                List.of("Point 1"),
                Instant.now()
        );

        assertThat(insightWithSupplyChain.hasSupplyChainData()).isTrue();
        assertThat(insightWithoutSupplyChain.hasSupplyChainData()).isFalse();
        assertThat(insightWithSupplyChain.ticker()).isEqualTo("AAPL");
    }
}
