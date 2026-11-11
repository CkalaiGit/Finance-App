package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.IngestionStatus;

import java.util.List;

/**
 * Port primaire exposé au reste de l'application (contrôleur REST).
 *
 * <p>Ce service fournit l'accès aux CompanyInsight consolidés et permet de demander
 * le démarrage contrôlé d'une ingestion.</p>
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

    /**
     * Récupère l'état des insights non expirés pour un ticker.
     *
     * @param ticker Symbole boursier
     * @return État d'ingestion calculé à partir des insights disponibles
     */
    IngestionStatus getStatus(String ticker);

    /**
     * Indique si la source de documents prend en charge ce ticker.
     *
     * @param ticker Symbole boursier
     * @return true si le ticker est connu de la source
     */
    boolean isTickerSupported(String ticker);

    /**
     * Demande une ingestion si le ticker n'est pas déjà en cours de traitement
     * et si le délai de garde autorise une nouvelle tentative.
     *
     * @param ticker Symbole boursier
     */
    void requestIngestion(String ticker);
}
