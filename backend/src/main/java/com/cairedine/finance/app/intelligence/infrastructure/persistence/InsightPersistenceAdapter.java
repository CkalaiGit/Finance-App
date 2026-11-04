package com.cairedine.finance.app.intelligence.infrastructure.persistence;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.port.ICompanyInsightRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class InsightPersistenceAdapter implements ICompanyInsightRepository {

    private final FinancialInsightJpaRepository repository;
    private final InsightPersistenceMapper mapper;
    private final int filingTtlDays;

    public InsightPersistenceAdapter(
            FinancialInsightJpaRepository repository,
            InsightPersistenceMapper mapper,
            @Value("${intelligence.filing-ttl-days:90}") int filingTtlDays) {
        this.repository = repository;
        this.mapper = mapper;
        this.filingTtlDays = filingTtlDays;
    }

    @Override
    @Transactional
    public void save(String ticker, CompanyInsight insight) {
        String normalizedTicker = normalizeTicker(ticker);
        Instant generatedAt = insight.generatedAt() == null ? Instant.now() : insight.generatedAt();
        FinancialInsightJpaEntity entity = repository
                .findByTickerAndFormTypeAndPeriod(normalizedTicker, insight.formType(), insight.period())
                .orElseGet(FinancialInsightJpaEntity::new);
        mapper.updateEntity(entity, normalizedTicker, insight, generatedAt);
        entity.setExpiresAt(generatedAt.plus(filingTtlDays, ChronoUnit.DAYS));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyInsight> findByTicker(String ticker) {
        return repository.findByTickerAndExpiresAtAfterOrderByGeneratedAtDesc(
                        normalizeTicker(ticker), Instant.now())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyInsight> findByTickerAndPeriod(String ticker, String formType, String period) {
        return repository.findByTickerAndFormTypeAndPeriod(normalizeTicker(ticker), formType, period)
                .filter(entity -> entity.getExpiresAt() != null && entity.getExpiresAt().isAfter(Instant.now()))
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccessionNumber(String accessionNumber) {
        return repository.existsByAccessionNumber(accessionNumber);
    }

    private String normalizeTicker(String ticker) {
        return ticker == null ? null : ticker.trim().toUpperCase(Locale.ROOT);
    }
}
