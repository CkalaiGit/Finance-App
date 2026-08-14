package com.cairedine.finance.app.intelligence.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modèle de domaine représentant un document d'entreprise ingéré et préparé pour l'indexation vectorielle.
 *
 * @param id Identifiant unique du document
 * @param ticker Symbole boursier de l'entreprise
 * @param source Source du document (ex: SEC_EDGAR)
 * @param accessionNumber Numéro d'accession SEC
 * @param formType Type de rapport (ex: 10-K, 10-Q)
 * @param period Période rapportée
 * @param content Contenu nettoyé du document
 * @param ingestedAt Date/heure d'ingestion
 * @param chunkCount Nombre de chunks générés
 */
public record CompanyDocument(
        String id,
        String ticker,
        String source,
        String accessionNumber,
        String formType,
        String period,
        String content,
        Instant ingestedAt,
        int chunkCount
) {
    /**
     * Factory statique construisant un CompanyDocument à partir d'un SecFiling.
     */
    public static CompanyDocument from(SecFiling filing, String ticker, int chunks) {
        return new CompanyDocument(
                UUID.randomUUID().toString(),
                ticker,
                "SEC_EDGAR",
                filing.accessionNumber(),
                filing.formType(),
                filing.period(),
                filing.rawContent(),
                Instant.now(),
                chunks
        );
    }
}
