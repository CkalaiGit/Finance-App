package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.infrastructure.sec.dto.SecCompanyTickerDto;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de résolution des symboles boursiers en CIK (Central Index Key) SEC EDGAR.
 * Le dictionnaire en mémoire est thread-safe pour les Virtual Threads.
 */
@Service
@Slf4j
public class CikResolverService {

    private final RestClient secWwwRestClient;
    private volatile Map<String, String> tickerToCik = Map.of();

    public CikResolverService(@Qualifier("secWwwRestClient") RestClient secWwwRestClient) {
        this.secWwwRestClient = secWwwRestClient;
    }

    @PostConstruct
    public void loadMappings() {
        log.info("Chargement du mapping officiel SEC Ticker -> CIK...");
        try {
            Map<String, SecCompanyTickerDto> response = secWwwRestClient.get()
                    .uri("/files/company_tickers.json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && !response.isEmpty()) {
                Map<String, String> map = new HashMap<>(response.size());
                for (SecCompanyTickerDto dto : response.values()) {
                    if (dto.ticker() != null && dto.cikStr() != null) {
                        map.put(dto.ticker().trim().toUpperCase(), dto.formattedCik());
                    }
                }
                this.tickerToCik = Collections.unmodifiableMap(map);
                log.info("Mapping SEC chargé avec succès : {} entreprises référencées.", this.tickerToCik.size());
            } else {
                log.warn("La réponse du fichier SEC company_tickers.json est vide.");
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement des mappings SEC company_tickers.json : {}", e.getMessage());
        }
    }

    /**
     * Résout un ticker boursier en identifiant CIK à 10 chiffres.
     *
     * @param ticker Ticker de l'entreprise (ex: "AAPL")
     * @return Identifiant CIK 10 chiffres (ex: "0000320193")
     * @throws TickerNotFoundException si le ticker n'existe pas dans le référentiel SEC
     */
    public String resolve(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new TickerNotFoundException("null");
        }
        String normalized = ticker.trim().toUpperCase();
        String cik = tickerToCik.get(normalized);
        if (cik == null) {
            log.warn("Ticker '{}' introuvable dans le référentiel SEC EDGAR", normalized);
            throw new TickerNotFoundException(normalized);
        }
        return cik;
    }

    /**
     * Indique si un ticker est répertorié par la SEC.
     */
    public boolean isKnown(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return false;
        }
        return tickerToCik.containsKey(ticker.trim().toUpperCase());
    }

    /**
     * Injection manuelle de mappings (usage interne / tests unitaires).
     */
    void setTickerToCik(Map<String, String> mappings) {
        this.tickerToCik = Map.copyOf(mappings);
    }
}