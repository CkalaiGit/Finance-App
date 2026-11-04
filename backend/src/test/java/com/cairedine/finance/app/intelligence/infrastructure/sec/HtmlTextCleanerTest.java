package com.cairedine.finance.app.intelligence.infrastructure.sec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextCleanerTest {

    private final HtmlTextCleaner cleaner = new HtmlTextCleaner();

    @Test
    void shouldExtractReadableTextAndRemoveHtmlArtifacts() {
        String html = """
                <html>
                  <head>
                    <style>.hidden { display: none; }</style>
                    <script>ignoredScriptContent()</script>
                  </head>
                  <body>
                    <h1>Annual Report</h1>
                    <p>Revenue increased during the year.</p>
                    <p>1,234.00</p>
                    <p><ix:nonfraction name="us-gaap:Revenue">Revenue amount</ix:nonfraction></p>
                  </body>
                </html>
                """;

        String cleaned = cleaner.clean(html);

        assertThat(cleaned)
                .contains("Annual Report", "Revenue increased during the year.", "Revenue amount")
                .doesNotContain("<", ">", "ignoredScriptContent", "display: none", "1,234.00");
    }

    @Test
    void shouldReturnEmptyTextForBlankInput() {
        assertThat(cleaner.clean(" \n\t ")).isEmpty();
    }
}
