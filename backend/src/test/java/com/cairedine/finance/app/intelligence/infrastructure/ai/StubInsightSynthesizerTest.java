package com.cairedine.finance.app.intelligence.infrastructure.ai;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.intelligence.domain.model.InsightSection;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StubInsightSynthesizerTest {

    @Test
    void createsDeterministicDemoOutputFromSectionsAndMetrics() {
        StubInsightSynthesizer synthesizer = new StubInsightSynthesizer(
                Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC));
        SecFiling filing = new SecFiling("accession-1", "10-K", "2024-12-31", "", Instant.EPOCH);
        List<SectionContent> sections = List.of(
                new SectionContent(InsightSection.RISK_FACTORS, "Risk text", 2),
                new SectionContent(InsightSection.GUIDANCE, "Guidance text", 3));
        List<FullMetrics> metrics = List.of(new FullMetrics(
                new GrowthMetrics(new BigDecimal("0.12"), null, null, null),
                new ValueMetrics(new BigDecimal("4.1"), null, null, null),
                new QualityMetrics(null, BigDecimal.ZERO, null, null, null),
                "2024-12-31", Instant.EPOCH));

        var first = synthesizer.synthesize("aapl", filing, sections, metrics);
        var second = synthesizer.synthesize("aapl", filing, sections, metrics);

        assertThat(first).isEqualTo(second);
        assertThat(first.synthesePerformance())
                .startsWith("[DÉMO] ")
                .contains("Risk Factors", "Guidance", "5 mots", "3 métrique(s)");
        assertThat(first.analyseMargesEtDette()).startsWith("[DÉMO] ");
        assertThat(first.risquesPrincipaux()).allSatisfy(text -> assertThat(text).startsWith("[DÉMO] "));
        assertThat(first.guidanceManagement()).startsWith("[DÉMO] ");
        assertThat(first.chaineApprovisionnement()).startsWith("[DÉMO] ");
        assertThat(first.faitsMarquants()).allSatisfy(text -> assertThat(text).startsWith("[DÉMO] "));
        assertThat(first.ticker()).isEqualTo("AAPL");
        assertThat(first.period()).isEqualTo("2024-12-31");
        assertThat(first.accessionNumber()).isEqualTo("accession-1");
    }
}
