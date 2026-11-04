package com.cairedine.finance.app.intelligence.infrastructure.sec;

import com.cairedine.finance.app.intelligence.domain.port.ITextCleaner;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HtmlTextCleaner implements ITextCleaner {

    private static final Pattern SCRIPT_AND_STYLE = Pattern.compile(
            "(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>"
    );
    private static final Pattern IX_TAGS = Pattern.compile("(?is)</?ix:[^>]+>");
    private static final Pattern HTML_TAGS = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern TABLE_ARTIFACT_LINE = Pattern.compile(
            "(?m)^[\\s\\d$\\p{P}\\p{S}]+$"
    );
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile("\\n{3,}");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \\t]{2,}");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x[\\da-f]+|\\d+);", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_ENTITY = Pattern.compile(
            "&(amp|lt|gt|quot|apos|nbsp);",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String clean(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        String extractedText;
        try {
            ByteArrayResource htmlResource =
                    new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8));
            List<Document> documents = new TikaDocumentReader(htmlResource).get();
            extractedText = documents.stream()
                    .map(Document::getText)
                    .reduce("", (first, second) -> first + "\n" + second);
            if (extractedText.isBlank()) {
                log.warn("Tika n'a extrait aucun texte du filing SEC; utilisation du nettoyage regex.");
                extractedText = fallbackToText(html);
            }
        } catch (RuntimeException exception) {
            log.warn("Échec du nettoyage Tika du filing SEC; utilisation du nettoyage regex.", exception);
            extractedText = fallbackToText(html);
        }

        return normalize(extractedText);
    }

    String fallbackToText(String html) {
        String withoutScriptAndStyle = SCRIPT_AND_STYLE.matcher(html).replaceAll(" ");
        String withoutIxTags = IX_TAGS.matcher(withoutScriptAndStyle).replaceAll(" ");
        String withoutHtmlTags = HTML_TAGS.matcher(withoutIxTags).replaceAll(" ");
        return decodeEntities(withoutHtmlTags);
    }

    private String decodeEntities(String text) {
        Matcher namedEntities = NAMED_ENTITY.matcher(text);
        StringBuilder decodedNamedEntities = new StringBuilder();
        while (namedEntities.find()) {
            String decoded = switch (namedEntities.group(1).toLowerCase()) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos" -> "'";
                case "nbsp" -> "\u00a0";
                default -> namedEntities.group();
            };
            namedEntities.appendReplacement(decodedNamedEntities, Matcher.quoteReplacement(decoded));
        }
        namedEntities.appendTail(decodedNamedEntities);

        Matcher numericEntities = NUMERIC_ENTITY.matcher(decodedNamedEntities.toString());
        StringBuilder decodedNumericEntities = new StringBuilder();
        while (numericEntities.find()) {
            String entityValue = numericEntities.group(1);
            int radix = entityValue.regionMatches(true, 0, "x", 0, 1) ? 16 : 10;
            String digits = radix == 16 ? entityValue.substring(1) : entityValue;
            String decoded;
            try {
                int codePoint = Integer.parseInt(digits, radix);
                decoded = Character.isValidCodePoint(codePoint)
                        ? new String(Character.toChars(codePoint))
                        : numericEntities.group();
            } catch (NumberFormatException exception) {
                decoded = numericEntities.group();
            }
            numericEntities.appendReplacement(decodedNumericEntities, Matcher.quoteReplacement(decoded));
        }
        numericEntities.appendTail(decodedNumericEntities);
        return decodedNumericEntities.toString();
    }

    private String normalize(String text) {
        String withoutTableArtifacts = TABLE_ARTIFACT_LINE.matcher(text).replaceAll("");
        String normalizedSpaces = MULTIPLE_SPACES.matcher(withoutTableArtifacts).replaceAll(" ");
        String normalizedLines = normalizedSpaces.replaceAll("\\r\\n?", "\n");
        return EXCESS_BLANK_LINES.matcher(normalizedLines).replaceAll("\n\n").trim();
    }
}
