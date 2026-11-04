package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.model.InsightSection;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import com.cairedine.finance.app.intelligence.domain.port.ISectionExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SectionExtractor implements ISectionExtractor {

    private static final int MIN_SECTION_LENGTH = 500;
    private static final int MAX_SECTION_LENGTH = 15_000;
    private static final int MAX_HEADING_LENGTH = 100;
    private static final int KEY_HIGHLIGHTS_LENGTH = 10_000;
    private static final Pattern NEXT_ITEM = Pattern.compile(
            "(?im)^[ \\t]*ITEM\\s+\\d+[A-Z]?\\b[^\\r\\n]*"
    );

    private static final List<SectionPattern> SECTION_PATTERNS = List.of(
            new SectionPattern(InsightSection.RISK_FACTORS,
                    "^[ \\t]*(?=.{1," + MAX_HEADING_LENGTH + "}$)"
                            + "(?:RISK FACTORS\\b[^\\r\\n]*|ITEM\\s+1A\\b[^\\r\\n]*)[ \\t]*$"),
            new SectionPattern(InsightSection.MANAGEMENT_DISCUSSION,
                    "^[ \\t]*(?=.{1," + MAX_HEADING_LENGTH + "}$)"
                            + "(?:MANAGEMENT['’]S\\s+DISCUSSION\\b[^\\r\\n]*|ITEM\\s+7\\b[^\\r\\n]*|MD&A\\b[^\\r\\n]*)[ \\t]*$"),
            new SectionPattern(InsightSection.GUIDANCE,
                    "^[ \\t]*(?=.{1," + MAX_HEADING_LENGTH + "}$)"
                            + "(?:OUTLOOK\\b[^\\r\\n]*|GUIDANCE\\b[^\\r\\n]*)[ \\t]*$"),
            new SectionPattern(InsightSection.SUPPLY_CHAIN,
                    "^[ \\t]*(?=.{1," + MAX_HEADING_LENGTH + "}$)"
                            + "(?:SUPPLY\\s+CHAIN\\b[^\\r\\n]*|MANUFACTURING\\b[^\\r\\n]*|SUPPLIERS\\b[^\\r\\n]*)[ \\t]*$")
    );

    private final int maxSectionsTokens;

    public SectionExtractor(
            @Value("${intelligence.summarization.max-sections-tokens:80000}") int maxSectionsTokens) {
        this.maxSectionsTokens = Math.max(0, maxSectionsTokens);
    }

    @Override
    public List<SectionContent> extractSections(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<SectionContent> sections = new ArrayList<>();
        for (SectionPattern sectionPattern : SECTION_PATTERNS) {
            extractSection(text, sectionPattern).ifPresent(sections::add);
        }

        if (sections.isEmpty()) {
            String highlights = text.substring(0, Math.min(text.length(), KEY_HIGHLIGHTS_LENGTH));
            sections.add(toSectionContent(InsightSection.KEY_HIGHLIGHTS, highlights));
        }

        return applyTokenLimit(sections);
    }

    @Override
    public int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 4;
    }

    private java.util.Optional<SectionContent> extractSection(String text, SectionPattern sectionPattern) {
        Matcher headingMatcher = Pattern.compile(
                        sectionPattern.expression(),
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                .matcher(text);

        while (headingMatcher.find()) {
            Matcher nextItemMatcher = NEXT_ITEM.matcher(text);
            nextItemMatcher.region(headingMatcher.end(), text.length());
            int end = nextItemMatcher.find() ? nextItemMatcher.start() : text.length();
            int sectionLength = end - headingMatcher.start();
            if (sectionLength < MIN_SECTION_LENGTH) {
                continue;
            }

            int boundedEnd = Math.min(end, headingMatcher.start() + MAX_SECTION_LENGTH);
            String sectionText = text.substring(headingMatcher.start(), boundedEnd).trim();
            return java.util.Optional.of(toSectionContent(sectionPattern.section(), sectionText));
        }

        return java.util.Optional.empty();
    }

    private List<SectionContent> applyTokenLimit(List<SectionContent> sections) {
        int totalTokens = sections.stream()
                .mapToInt(section -> estimateTokens(section.rawText()))
                .sum();
        if (totalTokens <= maxSectionsTokens) {
            return List.copyOf(sections);
        }

        log.warn("Les sections SEC dépassent le plafond de {} tokens; troncature des sections les plus longues.",
                maxSectionsTokens);

        List<SectionContent> truncatedSections = new ArrayList<>(sections);
        while (totalTokens > maxSectionsTokens) {
            int longestIndex = longestSectionIndex(truncatedSections);
            SectionContent longest = truncatedSections.get(longestIndex);
            int excessTokens = totalTokens - maxSectionsTokens;
            int charactersToRemove = Math.min(longest.rawText().length(), excessTokens * 4);
            String truncatedText = longest.rawText().substring(
                    0, longest.rawText().length() - charactersToRemove);
            truncatedSections.set(longestIndex, toSectionContent(longest.section(), truncatedText));
            totalTokens = truncatedSections.stream()
                    .mapToInt(section -> estimateTokens(section.rawText()))
                    .sum();
        }

        return List.copyOf(truncatedSections);
    }

    private int longestSectionIndex(List<SectionContent> sections) {
        return java.util.stream.IntStream.range(0, sections.size())
                .boxed()
                .max(Comparator.comparingInt(index -> sections.get(index).rawText().length()))
                .orElseThrow();
    }

    private SectionContent toSectionContent(InsightSection section, String text) {
        int wordCount = text.isBlank() ? 0 : text.trim().split("\\s+").length;
        return new SectionContent(section, text, wordCount);
    }

    private record SectionPattern(InsightSection section, String expression) {
    }
}
