package com.cairedine.finance.app.intelligence.infrastructure.ai;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

public class IntelligenceAiModeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        String mode = environment.getProperty("intelligence.ai.mode", "stub")
                .trim().toLowerCase(Locale.ROOT);
        if (!mode.equals("stub") && !mode.equals("gemini")) {
            throw new IllegalStateException(
                    "INTELLIGENCE_AI_MODE doit valoir 'stub' ou 'gemini', valeur reçue : '" + mode + "'.");
        }

        if (mode.equals("gemini")
                && !StringUtils.hasText(environment.getProperty("spring.ai.google.genai.api-key"))) {
            throw new IllegalStateException(
                    "Le mode Gemini nécessite GEMINI_API_KEY (clé API Google AI Studio).");
        }

        String chatModel = mode.equals("gemini") ? "google-genai" : "none";
        environment.getPropertySources().addFirst(new MapPropertySource(
                "intelligenceAiMode", Map.of("spring.ai.model.chat", chatModel)));
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
