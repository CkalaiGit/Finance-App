package com.cairedine.finance.app.intelligence.infrastructure.web;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.IngestionStatus;
import com.cairedine.finance.app.intelligence.domain.port.IAiSummaryService;
import com.cairedine.finance.app.user.domain.service.IUserSyncService;
import com.cairedine.finance.app.user.infrastructure.security.SecurityConfig;
import com.cairedine.finance.app.user.infrastructure.security.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsightController.class)
@Import({InsightWebMapper.class, SecurityConfig.class, WebConfig.class})
class InsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAiSummaryService aiSummaryService;

    @MockitoBean
    private IUserSyncService userSyncService;

    @Test
    void shouldReturn200WithInsightsWhenReady() throws Exception {
        Instant generatedAt = Instant.parse("2025-03-01T12:00:00Z");
        CompanyInsight insight = insight(generatedAt);
        when(aiSummaryService.isReady("AAPL")).thenReturn(true);
        when(aiSummaryService.getInsights("AAPL")).thenReturn(List.of(insight));

        mockMvc.perform(get("/api/v1/insights/aapl").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].formType").value("10-K"))
                .andExpect(jsonPath("$[0].synthesePerformance").value("Performance solide"))
                .andExpect(jsonPath("$[0].generatedAt").value("2025-03-01T12:00:00Z"));
    }

    @Test
    void shouldReturn202WhenIngestionPending() throws Exception {
        when(aiSummaryService.isReady("AAPL")).thenReturn(false);
        when(aiSummaryService.isTickerSupported("AAPL")).thenReturn(true);

        mockMvc.perform(get("/api/v1/insights/aapl").with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("INGESTION_PENDING"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturn404ForUnknownTicker() throws Exception {
        when(aiSummaryService.isReady("UNKNOWN")).thenReturn(false);
        when(aiSummaryService.isTickerSupported("UNKNOWN")).thenReturn(false);

        mockMvc.perform(get("/api/v1/insights/unknown").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidTicker() throws Exception {
        mockMvc.perform(get("/api/v1/insights/%20").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/insights/AAPL"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnStatus() throws Exception {
        Instant lastGenerated = Instant.parse("2025-03-01T12:00:00Z");
        when(aiSummaryService.isTickerSupported("AAPL")).thenReturn(true);
        when(aiSummaryService.getStatus("AAPL"))
                .thenReturn(new IngestionStatus("AAPL", true, true, 2, lastGenerated));

        mockMvc.perform(get("/api/v1/insights/aapl/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.inProgress").value(true))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.lastGenerated").value("2025-03-01T12:00:00Z"));
    }

    @Test
    void shouldReturn404ForUnknownTickerStatus() throws Exception {
        when(aiSummaryService.isTickerSupported("UNKNOWN")).thenReturn(false);

        mockMvc.perform(get("/api/v1/insights/unknown/status").with(jwt()))
                .andExpect(status().isNotFound());
    }

    private CompanyInsight insight(Instant generatedAt) {
        return new CompanyInsight(
                "AAPL",
                "10-K",
                "FY 2024",
                "0000000000-25-000001",
                "Performance solide",
                "Marges stables",
                List.of("Risque de marché"),
                null,
                null,
                List.of("Fait marquant"),
                generatedAt
        );
    }
}
