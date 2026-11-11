package com.cairedine.finance.app.intelligence.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntelligenceAiModeEnvironmentPostProcessorTest {

    private final IntelligenceAiModeEnvironmentPostProcessor processor =
            new IntelligenceAiModeEnvironmentPostProcessor();

    @Test
    void selectsNoChatModelInStubMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("intelligence.ai.mode", "stub")
                .withProperty("spring.ai.google.genai.api-key", "");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");
    }

    @Test
    void selectsGoogleGenAiInGeminiMode() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("intelligence.ai.mode", "gemini")
                .withProperty("spring.ai.google.genai.api-key", "test-key");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("google-genai");
    }

    @Test
    void failsClearlyWhenGeminiModeHasNoApiKey() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("intelligence.ai.mode", "gemini")
                .withProperty("spring.ai.google.genai.api-key", "");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }
}
