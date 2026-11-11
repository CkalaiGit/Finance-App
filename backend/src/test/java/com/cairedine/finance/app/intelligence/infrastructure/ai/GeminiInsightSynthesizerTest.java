package com.cairedine.finance.app.intelligence.infrastructure.ai;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.intelligence.domain.model.InsightSection;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GeminiInsightSynthesizerTest {

    @Test
    void trustsTickerAndFilingMetadataAndIncludesSectionsAndAvailableMetricsInPrompt() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        AiInsightPayload payload = new AiInsightPayload(
                "Synthèse produite", "Analyse produite", List.of("Risque"), "Guidance", null, List.of("Fait"));
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt().system(anyString()).user(prompt.capture()).call().entity(AiInsightPayload.class))
                .thenReturn(payload);
        GeminiInsightSynthesizer synthesizer = new GeminiInsightSynthesizer(builder,
                Clock.fixed(Instant.parse("2025-01-02T03:04:05Z"), ZoneOffset.UTC));

        SecFiling filing = new SecFiling("accession-from-filing", "10-K", "2024-12-31",
                "filing text", Instant.EPOCH);
        FullMetrics metrics = new FullMetrics(
                new GrowthMetrics(null, null, null, null),
                new ValueMetrics(null, new BigDecimal("25.4"), null, null),
                new QualityMetrics(null, null, null, null, null),
                "2024-12-31", Instant.EPOCH);
        var result = synthesizer.synthesize("aapl", filing,
                List.of(new SectionContent(InsightSection.MANAGEMENT_DISCUSSION, "Management text", 2)),
                List.of(metrics));

        assertThat(result.ticker()).isEqualTo("AAPL");
        assertThat(result.period()).isEqualTo("2024-12-31");
        assertThat(result.accessionNumber()).isEqualTo("accession-from-filing");
        assertThat(result.synthesePerformance()).isEqualTo("Synthèse produite");
        assertThat(result.generatedAt()).isEqualTo(Instant.parse("2025-01-02T03:04:05Z"));
        assertThat(prompt.getValue())
                .contains("2024-12-31", "accession-from-filing", "Management Discussion", "Management text",
                        "PE (TTM): 25.4")
                .doesNotContain("ROIC:");
    }

    @Test
    void throwsWhenModelReturnsNullPayload() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().entity(AiInsightPayload.class))
                .thenReturn(null);
        GeminiInsightSynthesizer synthesizer = new GeminiInsightSynthesizer(builder, Clock.systemUTC());

        assertThatThrownBy(() -> synthesizer.synthesize("AAPL",
                new SecFiling("accession", "10-Q", "2025-03-31", "", Instant.EPOCH),
                List.of(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accession");
    }
}
