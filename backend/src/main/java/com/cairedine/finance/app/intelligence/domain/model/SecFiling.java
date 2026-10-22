package com.cairedine.finance.app.intelligence.domain.model;

import java.time.Instant;

/**
 * Modèle de domaine représentant un rapport brut extrait de SEC EDGAR.
 *
 * @param accessionNumber Numéro d'accession unique du filing SEC
 * @param formType Type de formulaire (ex: 10-K, 10-Q)
 * @param period Période rapportée (ex: 2024-12-31)
 * @param rawContent Contenu brut (HTML/Texte) du filing
 * @param fetchedAt Horodatage de la récupération
 */
public record SecFiling(
        String accessionNumber,
        String formType,
        String period,
        String rawContent,
        Instant fetchedAt
) {
}
