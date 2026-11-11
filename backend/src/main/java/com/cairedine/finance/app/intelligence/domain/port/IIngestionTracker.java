package com.cairedine.finance.app.intelligence.domain.port;

/**
 * Port de consultation de l'état d'une ingestion en cours.
 */
public interface IIngestionTracker {

    boolean isIngesting(String ticker);
}
