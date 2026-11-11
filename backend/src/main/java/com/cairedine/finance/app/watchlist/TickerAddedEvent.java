package com.cairedine.finance.app.watchlist;

import java.time.Instant;

public record TickerAddedEvent(String ticker, Instant occurredAt) {
}
