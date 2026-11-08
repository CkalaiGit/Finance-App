package com.cairedine.finance.app.intelligence.infrastructure.persistence;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "intelligence.filing-ttl-days=14")
@ActiveProfiles("test")
@Import({InsightPersistenceAdapter.class, InsightPersistenceMapper.class})
class InsightPersistenceAdapterTest {

    @Autowired
    private InsightPersistenceAdapter adapter;

    @Autowired
    private FinancialInsightJpaRepository repository;

    @Test
    void shouldUpsertInsightUsingNormalizedTickerAndConfiguredTtl() {
        Instant generatedAt = Instant.now();
        adapter.save("aapl", insight(
                "10-K", "2024", "accession-first", "Initial summary", generatedAt));
        adapter.save("AAPL", insight(
                "10-K", "2024", "accession-second", "Updated summary", generatedAt.plusSeconds(1)));

        assertThat(repository.count()).isEqualTo(1);
        CompanyInsight stored = adapter.findByTicker("AAPL").getFirst();
        assertThat(stored.ticker()).isEqualTo("AAPL");
        assertThat(stored.accessionNumber()).isEqualTo("accession-second");
        assertThat(stored.synthesePerformance()).isEqualTo("Updated summary");

        FinancialInsightJpaEntity entity = repository.findByTickerAndFormTypeAndPeriod(
                "AAPL", "10-K", "2024").orElseThrow();
        assertThat(entity.getExpiresAt()).isEqualTo(generatedAt.plusSeconds(1).plus(14, ChronoUnit.DAYS));
    }

    @Test
    void shouldExcludeExpiredInsightsAndOnlyCheckNonExpiredAccessions() {
        Instant now = Instant.now();
        adapter.save("AAPL", insight(
                "10-K", "2023", "expired-accession", "Expired", now.minus(30, ChronoUnit.DAYS)));
        adapter.save("AAPL", insight(
                "10-Q", "2024-Q1", "active-accession", "Current", now));

        List<CompanyInsight> available = adapter.findByTicker("aapl");

        assertThat(available).extracting(CompanyInsight::accessionNumber)
                .containsExactly("active-accession");
        assertThat(adapter.existsByAccessionNumber("expired-accession")).isFalse();
        assertThat(adapter.existsByAccessionNumber("active-accession")).isTrue();
        assertThat(adapter.existsByAccessionNumber("unknown-accession")).isFalse();
    }

    @Test
    void shouldRefreshExpiredInsightOnTheSameRow() {
        Instant expiredGeneratedAt = Instant.now().minus(30, ChronoUnit.DAYS);
        adapter.save("AAPL", insight(
                "10-K", "2023", "expired-accession", "Old summary", expiredGeneratedAt));
        FinancialInsightJpaEntity expiredEntity = repository.findByTickerAndFormTypeAndPeriod(
                "AAPL", "10-K", "2023").orElseThrow();
        Long originalId = expiredEntity.getId();
        Instant expiredAt = expiredEntity.getExpiresAt();
        Instant refreshedGeneratedAt = Instant.now();

        adapter.save("AAPL", insight(
                "10-K", "2023", "refreshed-accession", "Refreshed summary", refreshedGeneratedAt));

        FinancialInsightJpaEntity refreshedEntity = repository.findByTickerAndFormTypeAndPeriod(
                "AAPL", "10-K", "2023").orElseThrow();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(refreshedEntity.getId()).isEqualTo(originalId);
        assertThat(refreshedEntity.getAccessionNumber()).isEqualTo("refreshed-accession");
        assertThat(refreshedEntity.getExpiresAt())
                .isEqualTo(refreshedGeneratedAt.plus(14, ChronoUnit.DAYS))
                .isAfter(expiredAt);
        assertThat(adapter.existsByAccessionNumber("expired-accession")).isFalse();
        assertThat(adapter.existsByAccessionNumber("refreshed-accession")).isTrue();
    }

    private CompanyInsight insight(
            String formType,
            String period,
            String accessionNumber,
            String summary,
            Instant generatedAt) {
        return new CompanyInsight(
                "AAPL",
                formType,
                period,
                accessionNumber,
                summary,
                null,
                List.of(),
                null,
                null,
                List.of(),
                generatedAt
        );
    }
}
