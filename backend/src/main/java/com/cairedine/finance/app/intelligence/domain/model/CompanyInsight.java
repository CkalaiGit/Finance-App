package com.cairedine.finance.app.intelligence.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Domaine : rapport consolidé hybride (quant + extraits SEC) — record immuable.
 */
public record CompanyInsight(
        String ticker,
        String formType,
        String period,
        String accessionNumber,

        String synthesePerformance,
        String analyseMargesEtDette,
        List<String> risquesPrincipaux,
        String guidanceManagement,
        String chaineApprovisionnement,
        List<String> faitsMarquants,

        Instant generatedAt
) {

    public boolean hasSupplyChainData() {
        return chaineApprovisionnement != null && !chaineApprovisionnement.isBlank();
    }

}
