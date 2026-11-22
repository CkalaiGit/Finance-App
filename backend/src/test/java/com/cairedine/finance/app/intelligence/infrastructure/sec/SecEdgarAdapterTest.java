package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import com.cairedine.finance.app.intelligence.domain.model.FilingReference;
import com.cairedine.finance.app.shared.exceptions.TickerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SecEdgarAdapterTest {

    private MockRestServiceServer dataMockServer;
    private MockRestServiceServer wwwMockServer;
    private SecEdgarAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder dataBuilder = RestClient.builder().baseUrl("https://data.sec.gov");
        this.dataMockServer = MockRestServiceServer.bindTo(dataBuilder).build();
        RestClient dataClient = dataBuilder.build();

        RestClient.Builder wwwBuilder = RestClient.builder().baseUrl("https://www.sec.gov");
        this.wwwMockServer = MockRestServiceServer.bindTo(wwwBuilder).build();
        RestClient wwwClient = wwwBuilder.build();

        CikResolverService cikResolver = new CikResolverService(wwwClient);
        cikResolver.setTickerToCik(Map.of("AAPL", "0000320193", "NVDA", "0001045810"));

        this.adapter = new SecEdgarAdapter(dataClient, wwwClient, cikResolver);
    }

    @Test
    void listFilingsReturnsReferencesNewestFirstRespectingLimit() {
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
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        List<FilingReference> filings = adapter.listFilings("AAPL", "10-Q", 1);

        dataMockServer.verify();
        wwwMockServer.verify();

        assertThat(filings).containsExactly(new FilingReference(
                "0000320193-25-000080",
                "10-Q",
                "2025-08-01",
                "aapl-20250628.htm"
        ));
    }

    @Test
    void listFilingsMatchesFormCaseInsensitivelyAndIgnoresAmendments() {
        String submissionsJson = """
                {
                  "cik": "0000320193",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000200", "0000320193-25-000106"],
                      "filingDate": ["2025-11-01", "2025-10-31"],
                      "form": ["10-K/A", "10-K"],
                      "primaryDocument": ["amended.htm", "aapl-20251031.htm"]
                    }
                  }
                }
                """;

        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        List<FilingReference> filings = adapter.listFilings("AAPL", "10-k", 5);

        dataMockServer.verify();
        wwwMockServer.verify();

        assertThat(filings).containsExactly(new FilingReference(
                "0000320193-25-000106",
                "10-K",
                "2025-10-31",
                "aapl-20251031.htm"
        ));
    }

    @Test
    void listFilingsReturnsEmptyWhenSubmissionsRequestFails() {
        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andRespond(withServerError());

        assertThat(adapter.listFilings("AAPL", "10-Q", 1)).isEmpty();

        dataMockServer.verify();
        wwwMockServer.verify();
    }

    @Test
    void listFilingsReturnsEmptyWhenRecentFilingsAbsent() {
        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andRespond(withSuccess("""
                        {"cik":"0000320193","filings":{}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.listFilings("AAPL", "10-Q", 1)).isEmpty();

        dataMockServer.verify();
        wwwMockServer.verify();
    }

    @Test
    void listFilingsHandlesMissingPrimaryDocumentAndDateArrays() {
        String submissionsJson = """
                {
                  "cik": "0000320193",
                  "filings": {
                    "recent": {
                      "accessionNumber": ["0000320193-25-000080", "0000320193-25-000050"],
                      "filingDate": ["2025-08-01"],
                      "form": ["10-Q", "10-Q"]
                    }
                  }
                }
                """;
        dataMockServer.expect(requestTo("https://data.sec.gov/submissions/CIK0000320193.json"))
                .andRespond(withSuccess(submissionsJson, MediaType.APPLICATION_JSON));

        AtomicReference<List<FilingReference>> filings = new AtomicReference<>();
        assertThatCode(() -> filings.set(adapter.listFilings("AAPL", "10-Q", 5)))
                .doesNotThrowAnyException();

        dataMockServer.verify();
        wwwMockServer.verify();
        assertThat(filings.get()).containsExactly(
                new FilingReference("0000320193-25-000080", "10-Q", "2025-08-01", null),
                new FilingReference("0000320193-25-000050", "10-Q", "UNKNOWN", null)
        );
    }

    @Test
    void downloadFilingReturnsSecFilingWithPrimaryDocument() {
        String htmlContent = "<html><body><h1>Apple Inc. Form 10-K Annual Report</h1></body></html>";
        FilingReference reference = new FilingReference(
                "0000320193-25-000106",
                "10-K",
                "2025-10-31",
                "aapl-20251031.htm"
        );
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/aapl-20251031.htm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(htmlContent, MediaType.TEXT_HTML));

        Optional<SecFiling> result = adapter.downloadFiling("AAPL", reference);

        dataMockServer.verify();
        assertThat(result).isPresent();
        SecFiling filing = result.orElseThrow();
        assertThat(filing.accessionNumber()).isEqualTo("0000320193-25-000106");
        assertThat(filing.formType()).isEqualTo("10-K");
        assertThat(filing.period()).isEqualTo("2025-10-31");
        assertThat(filing.rawContent()).isEqualTo(htmlContent);
        assertThat(filing.fetchedAt()).isNotNull();
        wwwMockServer.verify();
    }

    @Test
    void downloadFilingFallsBackToIndexJsonWhenPrimaryDocumentIsNull() {
        String indexJson = """
                {
                  "directory": {
                    "item": [
                      { "name": "form10k.htm", "type": "10-K", "size": "15000" }
                    ]
                  }
                }
                """;
        FilingReference reference = new FilingReference(
                "0000320193-25-000106",
                "10-K",
                "2025-10-31",
                null
        );
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/0000320193-25-000106-index.json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(indexJson, MediaType.APPLICATION_JSON));
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/form10k.htm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("<html>Fallback Content</html>", MediaType.TEXT_HTML));

        Optional<SecFiling> result = adapter.downloadFiling("AAPL", reference);

        dataMockServer.verify();
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().rawContent()).isEqualTo("<html>Fallback Content</html>");
        wwwMockServer.verify();
    }

    @Test
    void downloadFilingReturnsEmptyWhenContentIsBlank() {
        FilingReference reference = new FilingReference(
                "0000320193-25-000106",
                "10-K",
                "2025-10-31",
                "aapl-20251031.htm"
        );
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/aapl-20251031.htm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("   ", MediaType.TEXT_HTML));

        assertThat(adapter.downloadFiling("AAPL", reference)).isEmpty();

        dataMockServer.verify();
        wwwMockServer.verify();
    }

    @Test
    void downloadFilingReturnsEmptyWhenRequestFails() {
        FilingReference reference = new FilingReference(
                "0000320193-25-000106",
                "10-K",
                "2025-10-31",
                "aapl-20251031.htm"
        );
        wwwMockServer.expect(requestTo("https://www.sec.gov/Archives/edgar/data/320193/000032019325000106/aapl-20251031.htm"))
                .andRespond(withServerError());

        assertThat(adapter.downloadFiling("AAPL", reference)).isEmpty();

        dataMockServer.verify();
        wwwMockServer.verify();
    }

    @Test
    void listFilingsAndDownloadFilingThrowTickerNotFoundForUnknownTicker() {
        FilingReference reference = new FilingReference(
                "0000000000-25-000001",
                "10-K",
                "2025-10-31",
                "report.htm"
        );

        assertThatThrownBy(() -> adapter.listFilings("UNKNOWN", "10-K", 1))
                .isInstanceOf(TickerNotFoundException.class);
        assertThatThrownBy(() -> adapter.downloadFiling("UNKNOWN", reference))
                .isInstanceOf(TickerNotFoundException.class);

        dataMockServer.verify();
        wwwMockServer.verify();
    }

    @Test
    @DisplayName("isAvailable vérifie l'existence du ticker via CikResolver")
    void shouldCheckAvailability() {
        assertThat(adapter.isAvailable("AAPL")).isTrue();
        assertThat(adapter.isAvailable("UNKNOWN")).isFalse();
    }
}
