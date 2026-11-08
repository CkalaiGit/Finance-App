package com.cairedine.finance.app.intelligence.domain.model;

import java.time.Instant;

public record IngestionStatus(
        String ticker,
        boolean ready,
        int count,
        Instant lastGenerated
) {
}
