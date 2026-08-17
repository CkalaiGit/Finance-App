package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecEdgarAdapterLiveIntTest {

    private SecEdgarAdapter adapter;
    private CikResolverService cikResolver;

    @BeforeEach
    void setUp() {
        SecRestClientConfig config = new SecRestClientConfig();
        String userAgent = "CairedineFinance contact@cairedine.com";
        
        RestClient secDataClient = config.secDataRestClient("https://data.sec.gov", userAgent);
        RestClient secWwwClient = config.secWwwRestClient("https://www.sec.gov", userAgent);

        this.cikResolver = new CikResolverService(secWwwClient);
        this.cikResolver.loadMappings();

        this.adapter = new SecEdgarAdapter(secDataClient, secWwwClient, cikResolver);
    }

    @Test
    @DisplayName("Test Réel SEC EDGAR : Téléchargement du dernier 10-K de NVIDIA (NVDA)")
    void shouldFetchReal10KFilingForNvidia() {
        // 1. Vérification que NVDA est bien référencé dans les listings SEC
        assertThat(adapter.isAvailable("NVDA")).isTrue();

        // 2. Récupération du dernier rapport annuel 10-K
        List<SecFiling> filings = adapter.fetchFilings("NVDA", "10-K", 1);

        // 3. Assertions sur le résultat
        assertThat(filings).isNotEmpty();
        SecFiling filing = filings.getFirst();

        System.out.println("=================================================");
        System.out.println("✅ RAPPORT SEC 10-K RÉCUPÉRÉ AVEC SUCCÈS :");
        System.out.println(" - Ticker         : NVDA");
        System.out.println(" - Accession N°   : " + filing.accessionNumber());
        System.out.println(" - Date de dépôt  : " + filing.period());
        System.out.println(" - Formulaire     : " + filing.formType());
        System.out.println(" - Taille contenu : " + (filing.rawContent().length() / 1024) + " KB (" + filing.rawContent().length() + " caractères)");
        System.out.println("=================================================");

        assertThat(filing.formType()).isEqualTo("10-K");
        assertThat(filing.accessionNumber()).isNotBlank();
        assertThat(filing.rawContent()).isNotBlank();
        assertThat(filing.rawContent().length()).isGreaterThan(50_000);
        assertThat(filing.rawContent().toUpperCase()).contains("NVIDIA");
    }
}
