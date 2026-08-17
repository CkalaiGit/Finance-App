package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SecEdgarAdapterTest {

    private MockRestServiceServer dataMockServer;
    private MockRestServiceServer wwwMockServer;
    private CikResolverService cikResolver;
    private SecEdgarAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder dataBuilder = RestClient.builder().baseUrl("https://data.sec.gov");
        this.dataMockServer = MockRestServiceServer.bindTo(dataBuilder).build();
        RestClient dataClient = dataBuilder.build();

        RestClient.Builder wwwBuilder = RestClient.builder().baseUrl("https://www.sec.gov");
        this.wwwMockServer = MockRestServiceServer.bindTo(wwwBuilder).build();
        RestClient wwwClient = wwwBuilder.build();

        this.cikResolver = new CikResolverService(wwwClient);
        this.cikResolver.setTickerToCik(Map.of("AAPL", "0000320193", "NVDA", "0001045810"));

        this.adapter = new SecEdgarAdapter(dataClient, wwwClient, cikResolver);
    }

    @Test
    @DisplayName("Récupération nominale d'un rapport 10-K avec primaryDocument direct")
    void shouldFetchFilingsSuccessfullyWithPrimaryDocument() {
        String submissionsJson = """
                {
                  "cik": "0000320193",
                  "name": "Apple Inc.",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000106", "0000320193-25-000080", "0000320193-25-000050"],
                      "filingDate": ["2025-10-31", "2025-08-01", "2025-05-02"],
                      "form": ["10-K", "10-Q", "10-Q"],
                      "primaryDocument": ["aapl-20251031.htm", "aapl-20250628.htm", "aapl-20250329.htm"]
                    }
                  }
                }
                """;

        String htmlContent = "<html><body><h1>Apple Inc. Form 10-K Annual Report</h1></body></html>";

        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/aapl-20251031.htm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(htmlContent, MediaType.TEXT_HTML));

        List<SecFiling> filings = adapter.fetchFilings("AAPL", "10-K", 1);

        dataMockServer.verify();
        wwwMockServer.verify();

        assertThat(filings).hasSize(1);
        SecFiling filing = filings.getFirst();
        assertThat(filing.accessionNumber()).isEqualTo("0000320193-25-000106");
        assertThat(filing.formType()).isEqualTo("10-K");
        assertThat(filing.period()).isEqualTo("2025-10-31");
        assertThat(filing.rawContent()).contains("Apple Inc. Form 10-K Annual Report");
    }

    @Test
    @DisplayName("Récupération de plusieurs rapports 10-Q en respectant la limite")
    void shouldFetchMultipleFilingsRespectingLimit() {
        String submissionsJson = """
                {
                  "cik": "0000320193",
                  "name": "Apple Inc.",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000106", "0000320193-25-000080", "0000320193-25-000050"],
                      "filingDate": ["2025-10-31", "2025-08-01", "2025-05-02"],
                      "form": ["10-K", "10-Q", "10-Q"],
                      "primaryDocument": ["aapl-20251031.htm", "aapl-20250628.htm", "aapl-20250329.htm"]
                    }
                  }
                }
                """;

        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000080/aapl-20250628.htm"))
                .andRespond(withSuccess("<html>10-Q Q3</html>", MediaType.TEXT_HTML));

        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000050/aapl-20250329.htm"))
                .andRespond(withSuccess("<html>10-Q Q2</html>", MediaType.TEXT_HTML));

        List<SecFiling> filings = adapter.fetchFilings("AAPL", "10-Q", 2);

        assertThat(filings).hasSize(2);
        assertThat(filings.get(0).accessionNumber()).isEqualTo("0000320193-25-000080");
        assertThat(filings.get(1).accessionNumber()).isEqualTo("0000320193-25-000050");
    }

    @Test
    @DisplayName("Fallback sur index.json lorsque primaryDocument est absent")
    void shouldFallbackToIndexJsonWhenPrimaryDocumentAbsent() {
        String submissionsJson = """
                {
                  "cik": "0000320193",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000106"],
                      "filingDate": ["2025-10-31"],
                      "form": ["10-K"],
                      "primaryDocument": [null]
                    }
                  }
                }
                """;

        String indexJson = """
                {
                  "directory": {
                    "item": [
                      { "name": "form10k.htm", "type": "10-K", "size": "15000" }
                    ]
                  }
                }
                """;

        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/0000320193-25-000106-index.json"))
                .andRespond(withSuccess(indexJson, MediaType.APPLICATION_JSON));

        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/form10k.htm"))
                .andRespond(withSuccess("<html>Fallback Content</html>", MediaType.TEXT_HTML));

        List<SecFiling> filings = adapter.fetchFilings("AAPL", "10-K", 1);

        assertThat(filings).hasSize(1);
        assertThat(filings.getFirst().rawContent()).isEqualTo("<html>Fallback Content</html>");
    }

    @Test
    @DisplayName("Résilience : un échec de téléchargement individuel ne bloque pas les autres")
    void shouldHandleIndividualDownloadFailureGracefully() {
        String submissionsJson = """
                {
                  "cik": "0000320193",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000080", "0000320193-25-000050"],
                      "filingDate": ["2025-08-01", "2025-05-02"],
                      "form": ["10-Q", "10-Q"],
                      "primaryDocument": ["doc1.htm", "doc2.htm"]
                    }
                  }
                }
                """;

        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        // Premier document échoue avec erreur 500
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000080/doc1.htm"))
                .andRespond(withServerError());

        // Deuxième document réussit
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000050/doc2.htm"))
                .andRespond(withSuccess("<html>Doc 2 OK</html>", MediaType.TEXT_HTML));

        List<SecFiling> filings = adapter.fetchFilings("AAPL", "10-Q", 2);

        assertThat(filings).hasSize(1);
        assertThat(filings.getFirst().accessionNumber()).isEqualTo("0000320193-25-000050");
    }

    @Test
    @DisplayName("isAvailable vérifie l'existence du ticker via CikResolver")
    void shouldCheckAvailability() {
        assertThat(adapter.isAvailable("AAPL")).isTrue();
        assertThat(adapter.isAvailable("UNKNOWN")).isFalse();
    }
}
