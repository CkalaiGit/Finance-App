package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.InsightType;

/**
 * Port primaire (cas d'usage) exposant les fonctionnalités de génération d'insights RAG via l'IA Gemini.
 */
public interface IAiInsightService {

    /**
     * Génère une synthèse d'insight RAG structurée pour une entreprise résumant les 10-K et le dernier 10-Q.
     *
     * @param ticker Symbole boursier de l'entreprise (ex: AAPL)
     * @param type Type d'analyse d'insight (EARNINGS_SUMMARY, RISK_FACTORS, CAPITAL_ALLOCATION)
     * @return L'insight structuré généré par l'IA Gemini
     */
    CompanyInsight generateInsight(String ticker, InsightType type);

    /**
     * Vérifie si les documents et vecteurs requis (10-K et dernier 10-Q) sont ingérés et prêts.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @return true si l'indexation est terminée et que l'IA est prête
     */
    boolean isReady(String ticker);
}
