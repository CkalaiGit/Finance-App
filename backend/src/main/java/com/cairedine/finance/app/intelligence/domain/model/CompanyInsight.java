package com.cairedine.finance.app.intelligence.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Modèle de domaine représentant l'analyse/insight qualitative générée par l'IA Gemini.
 *
 * @param ticker Symbole boursier de l'entreprise
 * @param type Type d'insight (EARNINGS_SUMMARY, RISK_FACTORS, CAPITAL_ALLOCATION)
 * @param summary Résumé global de l'analyse
 * @param keyPoints Points clés extraits des rapports
 * @param risks Facteurs de risque identifiés
 * @param outlook Perspectives d'avenir exprimées
 * @param sources Identifiants/références des documents SEC utilisés comme sources
 * @param generatedAt Date/heure de génération de l'insight
 */
public record CompanyInsight(
        String ticker,
        InsightType type,
        String summary,
        List<String> keyPoints,
        List<String> risks,
        String outlook,
        List<String> sources,
        Instant generatedAt
) {
    /**
     * Indique si l'insight repose sur des documents ingérés.
     */
    public boolean hasDocuments() {
        return sources != null && !sources.isEmpty();
    }
}
