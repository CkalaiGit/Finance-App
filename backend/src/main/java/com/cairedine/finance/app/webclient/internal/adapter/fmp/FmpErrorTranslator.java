package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import com.cairedine.finance.app.shared.exceptions.MarketDataUnavailableException;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
final class FmpErrorTranslator {

    private FmpErrorTranslator() {}

    static RuntimeException translate(String symbol, String endpoint, RestClientException exception) {
        if (exception instanceof HttpStatusCodeException responseException) {
            int status = responseException.getStatusCode().value();

            if (status == 404) {
                return new TickerNotFoundException(symbol);
            }
            if (status == 401 || status == 403 || status == 402) {
                log.error("FMP configuration failure: status={}, endpoint={}, symbol={}",
                        status, endpoint, symbol, exception);
            } else {
                log.warn("FMP request failed: status={}, endpoint={}, symbol={}",
                        status, endpoint, symbol, exception);
            }
        } else if (exception instanceof ResourceAccessException) {
            log.warn("FMP request failed: status unavailable, endpoint={}, symbol={}",
                    endpoint, symbol, exception);
        } else {
            log.warn("FMP response could not be processed: endpoint={}, symbol={}",
                    endpoint, symbol, exception);
        }

        return new MarketDataUnavailableException(symbol, exception);
    }
}
