package com.cairedine.finance.app.intelligence.application;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.IngestionStatus;
import com.cairedine.finance.app.intelligence.domain.port.IAiSummaryService;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import com.cairedine.finance.app.intelligence.domain.port.IDocumentSourcePort;
import com.cairedine.finance.app.intelligence.domain.port.IIngestionTracker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AiSummaryServiceImpl implements IAiSummaryService {

    private final ICompanyInsightRepository insightRepository;
    private final IDocumentSourcePort documentSource;
    private final IIngestionTracker ingestionTracker;
    private final IngestionLauncher ingestionLauncher;

    public AiSummaryServiceImpl(
            ICompanyInsightRepository insightRepository,
            IDocumentSourcePort documentSource,
            IIngestionTracker ingestionTracker,
            IngestionLauncher ingestionLauncher) {
        this.insightRepository = insightRepository;
        this.documentSource = documentSource;
        this.ingestionTracker = ingestionTracker;
        this.ingestionLauncher = ingestionLauncher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyInsight> getInsights(String ticker) {
        return insightRepository.findByTicker(normalizeTicker(ticker));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReady(String ticker) {
        return getStatus(ticker).ready();
    }

    @Override
    @Transactional(readOnly = true)
    public IngestionStatus getStatus(String ticker) {
        String normalizedTicker = normalizeTicker(ticker);
        List<CompanyInsight> insights = insightRepository.findByTicker(normalizedTicker);
        Instant lastGenerated = insights.stream()
                .map(CompanyInsight::generatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new IngestionStatus(normalizedTicker, !insights.isEmpty(),
                ingestionTracker.isIngesting(normalizedTicker), insights.size(), lastGenerated);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTickerSupported(String ticker) {
        return documentSource.isAvailable(normalizeTicker(ticker));
    }

    @Override
    public void requestIngestion(String ticker) {
        String normalizedTicker = normalizeTicker(ticker);
        if (!ingestionTracker.isIngesting(normalizedTicker)) {
            ingestionLauncher.launchIfAllowed(normalizedTicker);
        }
    }

    private String normalizeTicker(String ticker) {
        return ticker == null ? null : ticker.trim().toUpperCase(Locale.ROOT);
    }
}
