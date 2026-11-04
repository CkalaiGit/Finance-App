package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.SectionContent;

import java.util.List;

public interface ISectionExtractor {

    List<SectionContent> extractSections(String text);

    int estimateTokens(String text);
}
