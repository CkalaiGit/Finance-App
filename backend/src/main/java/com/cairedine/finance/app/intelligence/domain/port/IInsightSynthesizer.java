package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.SectionContent;
import com.cairedine.finance.app.intelligence.domain.model.SecFiling;

import java.util.List;

public interface IInsightSynthesizer {

    CompanyInsight synthesize(
            String ticker,
            SecFiling filing,
            List<SectionContent> sections,
            List<FullMetrics> metrics);
}
