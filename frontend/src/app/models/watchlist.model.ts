export interface WatchlistResponse {
   keycloakId: string;
   tickers: string[];
}

export interface TickerExistsResponse {
   ticker: string;
   exists: boolean;
}
