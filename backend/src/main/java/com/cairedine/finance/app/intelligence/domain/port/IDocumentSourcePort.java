package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.model.FilingReference;

import java.util.List;
import java.util.Optional;

/**
 * Port secondaire d'extraction de documents boursiers et administratifs bruts (ex: SEC EDGAR).
 */
public interface IDocumentSourcePort {

    /**
     * Récupère la liste des filings bruts pour un ticker et un type de formulaire donné.
     *
     * @param ticker Symbole boursier de l'entreprise (ex: AAPL)
     * @param formType Type de formulaire SEC (ex: 10-K, 10-Q)
     * @param limit Nombre maximal de documents à récupérer
     * @return Liste des filings bruts récupérés
     */
    List<SecFiling> fetchFilings(String ticker, String formType, int limit);

    List<FilingReference> listFilings(String ticker, String formType, int limit);

    Optional<SecFiling> downloadFiling(String ticker, FilingReference reference);

    /**
     * Vérifie si la source de documents est disponible pour le ticker spécifié.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @return true si le ticker est connu et supporté par la source
     */
    boolean isAvailable(String ticker);
}
