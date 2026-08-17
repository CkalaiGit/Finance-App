package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

class DebugSecJsonTest {

    @Test
    void debugJson() {
        SecRestClientConfig config = new SecRestClientConfig();
        RestClient client = config.secWwwRestClient("https://www.sec.gov", "CairedineFinance contact@cairedine.com");

        try {
            String rawJson = client.get().uri("/files/company_tickers.json").retrieve().body(String.class);
            System.out.println("Raw JSON length: " + rawJson.length());
            System.out.println("First 300 chars: " + rawJson.substring(0, Math.min(300, rawJson.length())));

            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> map = mapper.readValue(rawJson, Map.class);
            System.out.println("Map size: " + map.size());
            System.out.println("First entry: " + map.get("0"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
