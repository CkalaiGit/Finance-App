package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;

import java.util.List;
import java.util.Optional;

/**
 * Port secondaire de persistance des insights d'entreprise.
 *
 * <p>Le domaine reste pur : cette interface exprime les opérations nécessaires
 * pour stocker et retrouver des CompanyInsight sans exposer de détails techniques.</p>
 */
public interface ICompanyInsightRepository {

    /**
     * Sauvegarde ou met à jour le CompanyInsight pour un ticker donné.
     * Implémentations doivent garantir l'idempotence côté stockage lorsque
     * l'accessionNumber est déjà présent.
     *
     * @param ticker Symbole boursier de l'entreprise (ex: AAPL)
     * @param insight Objet immuable représentant le résumé généré
     */
    void save(String ticker, CompanyInsight insight);

    /**
     * Retourne la liste des insights non expirés pour le ticker.
     *
     * @param ticker Symbole boursier
     * @return Liste d'insights valides (peut être vide)
     */
    List<CompanyInsight> findByTicker(String ticker);

    /**
     * Cherche un insight précis pour un ticker, un type de formulaire et une période.
     *
     * @param ticker Symbole boursier
     * @param formType Type de formulaire (ex: 10-K, 10-Q)
     * @param period Période/année du rapport (format libre mais stable)
     * @return Optional contenant l'insight si trouvé et non expiré
     */
    Optional<CompanyInsight> findByTickerAndPeriod(String ticker, String formType, String period);

    /**
     * Vérifie l'existence d'un enregistrement par accessionNumber pour garantir
     * l'idempotence d'ingestions répétées.
     *
     * @param accessionNumber Identifiant d'accès du dépôt SEC (acc_no)
     * @return true si un enregistrement avec cet accessionNumber existe
     */
    boolean existsByAccessionNumber(String accessionNumber);
}
