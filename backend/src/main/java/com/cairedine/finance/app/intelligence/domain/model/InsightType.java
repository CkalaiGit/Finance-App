package com.cairedine.finance.app.intelligence.domain.model;

import lombok.Getter;

/**
 * Type d'analyse d'insight générée par l'IA Gemini.
 */
@Getter
public enum InsightType {

    EARNINGS_SUMMARY("Résume les résultats financiers, la croissance des revenus et les perspectives exprimées par le management."),
    RISK_FACTORS("Analyse les principaux facteurs de risque, les incertitudes et les défis mentionnés par la direction."),
    CAPITAL_ALLOCATION("Évalue la stratégie d'allocation du capital : rachat d'actions, dividendes, acquisitions et investissements CAPEX.");

    private final String promptInstruction;

    InsightType(String promptInstruction) {
        this.promptInstruction = promptInstruction;
    }

}
