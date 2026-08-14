package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyDocument;

import java.util.List;

/**
 * Port de persistance (Repository NoSQL/MongoDB) pour les documents d'entreprise nettoyés.
 */
public interface ICompanyDocumentRepository {

    /**
     * Enregistre un document d'entreprise nettoyé.
     *
     * @param document Le document à persister
     */
    void save(CompanyDocument document);

    /**
     * Recherche tous les documents enregistrés pour un ticker donné.
     *
     * @param ticker Symbole boursier de l'entreprise
     * @return Liste des documents associés au ticker
     */
    List<CompanyDocument> findByTicker(String ticker);

    /**
     * Vérifie si un document existe déjà en base via son numéro d'accession unique SEC.
     *
     * @param accessionNumber Numéro d'accession SEC
     * @return true si le document est déjà ingéré
     */
    boolean existsByAccessionNumber(String accessionNumber);

    /**
     * Supprime tous les documents associés à un ticker.
     *
     * @param ticker Symbole boursier de l'entreprise
     */
    void deleteByTicker(String ticker);
}
