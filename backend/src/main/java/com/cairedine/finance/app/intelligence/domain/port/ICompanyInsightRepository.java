package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;

import java.util.List;
import java.util.Optional;

/**
 * Port secondaire de persistance des synthèses d'insights d'entreprise.
 *
 * <p>Ce port abstrait le stockage PostgreSQL sous-jacent et permet au domaine
 * d'enregistrer et de relire les analyses hybrides générées par l'IA.</p>
 */
public interface ICompanyInsightRepository {

    /**
     * Sauvegarde ou met à jour la synthèse hybride pour un ticker donné.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @param insight Synthèse hybride consolidée
     */
    void save(String ticker, CompanyInsight insight);

    /**
     * Récupère toutes les synthèses non expirées disponibles pour un ticker.
     *
     * @param ticker Symbole boursier
     * @return Liste des synthèses disponibles
     */
    List<CompanyInsight> findByTicker(String ticker);

    /**
     * Recherche une synthèse pour un ticker, un type de rapport et une période précis.
     *
     * @param ticker Symbole boursier
     * @param formType Type de rapport SEC (10-K, 10-Q)
     * @param period Période fiscale (ex: FY 2025, Q1 2026)
     * @return Optional contenant la synthèse si trouvée
     */
    Optional<CompanyInsight> findByTickerAndPeriod(String ticker, String formType, String period);

    /**
     * Vérifie si un rapport a déjà été traité et sauvegardé via son numéro d'accession SEC (idempotence).
     *
     * @param accessionNumber Numéro d'accession officiel SEC
     * @return true si le document a déjà été persisté
     */
    boolean existsByAccessionNumber(String accessionNumber);
}
