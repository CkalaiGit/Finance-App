package com.cairedine.finance.app.intelligence.infrastructure.ai;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.InsightSection;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IInsightSynthesizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "intelligence.ai.mode", havingValue = "stub", matchIfMissing = true)
class StubInsightSynthesizer implements IInsightSynthesizer {

    private static final String DEMO = "[DÉMO] ";
    private final Clock clock;

    StubInsightSynthesizer(Clock clock) {
        this.clock = clock;
    }

    @Override
    public CompanyInsight synthesize(
            String ticker,
            SecFiling filing,
            List<SectionContent> sections,
            List<FullMetrics> metrics) {
        List<SectionContent> foundSections = sections == null ? List.of()
                : sections.stream()
                        .filter(Objects::nonNull)
                        .filter(section -> section.section() != null)
                        .toList();
        List<String> sectionNames = foundSections.stream()
                .map(section -> section.section().sectionTitle())
                .toList();
        int wordCount = foundSections.stream()
                .mapToInt(section -> Math.max(section.wordCount(), 0))
                .sum();
        int availableMetrics = metrics == null ? 0
                : metrics.stream().filter(Objects::nonNull).mapToInt(this::countMetrics).sum();

        String sectionsDescription = sectionNames.isEmpty()
                ? "aucune section trouvée"
                : String.join(", ", sectionNames);
        List<String> risks = sectionNames.contains(InsightSection.RISK_FACTORS.sectionTitle())
                ? List.of(DEMO + "Section Risk Factors disponible dans le filing.")
                : List.of(DEMO + "Aucune section Risk Factors n'a été trouvée.");
        List<String> highlights = sectionNames.stream()
                .map(name -> DEMO + "Section SEC trouvée : " + name + ".")
                .toList();
        if (highlights.isEmpty()) {
            highlights = List.of(DEMO + "Aucune section SEC n'a été trouvée.");
        }

        return new CompanyInsight(
                ticker == null ? null : ticker.trim().toUpperCase(Locale.ROOT),
                filing.formType(),
                filing.period(),
                filing.accessionNumber(),
                DEMO + "Synthèse déterministe : " + sectionsDescription + "; "
                        + wordCount + " mots et " + availableMetrics + " métrique(s) disponible(s).",
                DEMO + availableMetrics + " métrique(s) disponible(s) dans "
                        + (metrics == null ? 0 : metrics.stream().filter(Objects::nonNull).count())
                        + " exercice(s).",
                risks,
                DEMO + (sectionNames.contains(InsightSection.GUIDANCE.sectionTitle())
                        ? "Une section Guidance est disponible; consulter le filing source."
                        : "Aucune section Guidance n'a été trouvée."),
                DEMO + (sectionNames.contains(InsightSection.SUPPLY_CHAIN.sectionTitle())
                        ? "Une section Supply Chain est disponible; consulter le filing source."
                        : "Aucune section Supply Chain n'a été trouvée."),
                highlights,
                clock.instant());
    }

    private int countMetrics(FullMetrics metrics) {
        return count(metrics.growth() == null ? null : metrics.growth().revenueGrowth3Y(),
                metrics.growth() == null ? null : metrics.growth().ebitdaGrowth3Y(),
                metrics.growth() == null ? null : metrics.growth().epsGrowth3Y(),
                metrics.growth() == null ? null : metrics.growth().fcfGrowth(),
                metrics.value() == null ? null : metrics.value().evToEbit(),
                metrics.value() == null ? null : metrics.value().peRatioTTM(),
                metrics.value() == null ? null : metrics.value().pegRatioForward(),
                metrics.value() == null ? null : metrics.value().evToSales(),
                metrics.quality() == null ? null : metrics.quality().roic(),
                metrics.quality() == null ? null : metrics.quality().operatingMargin(),
                metrics.quality() == null ? null : metrics.quality().netDebtToEbitda(),
                metrics.quality() == null ? null : metrics.quality().freeCashFlowMargin(),
                metrics.quality() == null ? null : metrics.quality().sgaToRevenue());
    }

    private int count(BigDecimal... values) {
        int count = 0;
        for (BigDecimal value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }
}
