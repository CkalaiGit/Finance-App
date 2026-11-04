package com.cairedine.finance.app.intelligence.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface FinancialInsightJpaRepository extends JpaRepository<FinancialInsightJpaEntity, Long> {

    Optional<FinancialInsightJpaEntity> findByTickerAndFormTypeAndPeriod(
            String ticker, String formType, String period);

    List<FinancialInsightJpaEntity> findByTickerAndExpiresAtAfterOrderByGeneratedAtDesc(
            String ticker, Instant expiresAt);

    boolean existsByAccessionNumber(String accessionNumber);
}
