package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository;

import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IFinancialAnalysisRepository extends JpaRepository<FinancialAnalysisJpaEntity, Long> {

    List<FinancialAnalysisJpaEntity> findAllByTickerOrderByFiscalYearEndDateDesc(String ticker);

    Optional<FinancialAnalysisJpaEntity> findByTickerAndFiscalYearEndDate(String ticker, String fiscalYearEndDate);
}
