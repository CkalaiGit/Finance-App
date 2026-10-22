package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;

import java.util.List;

/**
 * Port primaire exposé au reste de l'application (contrôleur REST).
 *
 * <p>Ce service est en lecture seule du point de vue du contrôleur : il fournit
 * l'accès aux CompanyInsight consolidés. L'ingestion et la génération
 * d'insights sont gérées en interne et ne sont pas exposées via ce port.</p>
 */
public interface IAiSummaryService {

    /**
     * Récupère les insights disponibles pour un ticker donné.
     *
     * @param ticker Symbole boursier (ex: AAPL)
     * @return Liste d'insights prêts à être retournés au client (peut être vide)
     */
    List<CompanyInsight> getInsights(String ticker);

    /**
     * Indique si au moins un rapport complet (insight) est disponible
     * pour le ticker — utile pour indiquer l'état readiness au contrôleur.
     *
     * @param ticker Symbole boursier
     * @return true si au moins un insight complet est disponible
     */
    boolean isReady(String ticker);
}
