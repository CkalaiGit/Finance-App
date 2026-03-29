package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository;

import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IFinancialAnalysisRepository extends JpaRepository<FinancialAnalysisJpaEntity, Long> {
    Optional<FinancialAnalysisJpaEntity> findByTicker(String ticker);
}
