package com.cairedine.finance.app.intelligence.application;

import java.util.List;

record AiInsightPayload(
        String synthesePerformance,
        String analyseMargesEtDette,
        List<String> risquesPrincipaux,
        String guidanceManagement,
        String chaineApprovisionnement,
        List<String> faitsMarquants
) {
}
