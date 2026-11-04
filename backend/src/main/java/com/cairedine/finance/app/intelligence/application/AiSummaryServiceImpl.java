package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.InsightSection;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.port.IAiSummaryService;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class AiSummaryServiceImpl implements IAiSummaryService {

    private static final String SYSTEM_PROMPT = """
            Tu es un analyste financier expert. Croise les métriques quantitatives réelles fournies
            avec les extraits qualitatifs des rapports SEC 10-K/10-Q. Réponds en français et
            exclusivement avec un JSON strict conforme au schéma demandé par l'utilisateur.
            N'utilise que les chiffres présents dans les données fournies : n'invente, ne déduis
            et ne complète jamais une valeur numérique absente. Si une information n'est pas
            étayée par les extraits, indique qu'elle est indisponible ou omets-la selon le champ.
            """;

    private final ChatClient chatClient;
    private final ICompanyInsightRepository insightRepository;

    public AiSummaryServiceImpl(
            ChatClient.Builder chatClientBuilder,
            ICompanyInsightRepository insightRepository) {
        this.chatClient = chatClientBuilder.build();
        this.insightRepository = insightRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyInsight> getInsights(String ticker) {
        return insightRepository.findByTicker(normalizeTicker(ticker));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReady(String ticker) {
        return !insightRepository.findByTicker(normalizeTicker(ticker)).isEmpty();
    }

    public CompanyInsight summarize(
            String ticker,
            SecFiling filing,
            List<SectionContent> sections,
            List<FullMetrics> metrics) {
        AiInsightPayload payload = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserPrompt(sections, metrics))
                .call()
                .entity(AiInsightPayload.class);

        if (payload == null) {
            throw new IllegalStateException("Gemini n'a retourné aucune synthèse pour le filing "
                    + filing.accessionNumber());
        }

        return new CompanyInsight(
                normalizeTicker(ticker),
                filing.formType(),
                filing.period(),
                filing.accessionNumber(),
                payload.synthesePerformance(),
                payload.analyseMargesEtDette(),
                payload.risquesPrincipaux(),
                payload.guidanceManagement(),
                payload.chaineApprovisionnement(),
                payload.faitsMarquants(),
                Instant.now()
        );
    }

    private String buildUserPrompt(List<SectionContent> sections, List<FullMetrics> metrics) {
        StringBuilder prompt = new StringBuilder("Données quantitatives :\n")
                .append(formatMetrics(metrics))
                .append("\n\nExtraits qualitatifs du filing :\n");

        if (sections == null || sections.isEmpty()) {
            prompt.append("Aucune section qualitative n'a été isolée.\n");
        } else {
            for (SectionContent section : sections) {
                InsightSection sectionType = section.section();
                prompt.append("\n### ").append(sectionType.sectionTitle()).append('\n')
                        .append("Instruction d'analyse : ")
                        .append(sectionType.summaryInstruction()).append('\n')
                        .append(section.rawText() == null ? "" : section.rawText())
                        .append('\n');
            }
        }

        return prompt.append("""

                Réponds avec un objet JSON strict contenant uniquement ces champs :
                {
                  "synthesePerformance": "Synthèse croisée en français",
                  "analyseMargesEtDette": "Analyse des marges et de la dette",
                  "risquesPrincipaux": ["Risque étayé par le texte"],
                  "guidanceManagement": "Guidance étayée par le texte ou null",
                  "chaineApprovisionnement": null,
                  "faitsMarquants": ["Fait marquant étayé par le texte"]
                }
                Compare la période la plus récente aux deux périodes précédentes pour dégager
                une tendance uniquement lorsque ces périodes et les valeurs correspondantes existent.
                Fournis 5 à 8 risques et 3 à 5 faits marquants lorsqu'ils sont étayés par les
                données. Utilise null pour chaineApprovisionnement si les extraits ne contiennent
                aucune information sur la chaîne d'approvisionnement. Ne renvoie ni Markdown,
                ni commentaire hors du JSON.
                """).toString();
    }

    private String formatMetrics(List<FullMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "métriques quantitatives indisponibles";
        }

        List<FullMetrics> recentMetrics = metrics.stream()
                .filter(metric -> metric != null)
                .sorted(Comparator.comparing(
                        FullMetrics::fiscalYearEndDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .toList();
        if (recentMetrics.isEmpty()) {
            return "métriques quantitatives indisponibles";
        }

        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < recentMetrics.size(); index++) {
            FullMetrics metric = recentMetrics.get(index);
            formatted.append(index == 0 ? "Période la plus récente" : "Période précédente " + index)
                    .append(" (")
                    .append(StringUtils.hasText(metric.fiscalYearEndDate())
                            ? metric.fiscalYearEndDate()
                            : "période indisponible")
                    .append("):\n");
            appendMetric(formatted, "Marge opérationnelle", metric.quality() == null
                    ? null : metric.quality().operatingMargin());
            appendMetric(formatted, "ROIC", metric.quality() == null
                    ? null : metric.quality().roic());
            appendMetric(formatted, "Net Debt/EBITDA", metric.quality() == null
                    ? null : metric.quality().netDebtToEbitda());
            appendMetric(formatted, "FCF margin", metric.quality() == null
                    ? null : metric.quality().freeCashFlowMargin());
            appendMetric(formatted, "Croissance du chiffre d'affaires sur 3 ans",
                    metric.growth() == null ? null : metric.growth().revenueGrowth3Y());
            appendMetric(formatted, "Croissance EBITDA sur 3 ans",
                    metric.growth() == null ? null : metric.growth().ebitdaGrowth3Y());
            appendMetric(formatted, "Croissance EPS sur 3 ans",
                    metric.growth() == null ? null : metric.growth().epsGrowth3Y());
            appendMetric(formatted, "PE (TTM)", metric.value() == null
                    ? null : metric.value().peRatioTTM());
            appendMetric(formatted, "EV/EBIT", metric.value() == null
                    ? null : metric.value().evToEbit());
            appendMetric(formatted, "EV/Sales", metric.value() == null
                    ? null : metric.value().evToSales());
        }
        return formatted.toString().trim();
    }

    private void appendMetric(StringBuilder formatted, String label, BigDecimal value) {
        if (value != null) {
            formatted.append("- ").append(label).append(": ").append(value.toPlainString()).append('\n');
        }
    }

    private String normalizeTicker(String ticker) {
        return ticker == null ? null : ticker.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
