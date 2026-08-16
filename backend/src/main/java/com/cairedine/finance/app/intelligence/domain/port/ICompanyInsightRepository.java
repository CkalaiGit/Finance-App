package com.cairedine.finance.app.intelligence.domain.port;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;

import java.util.List;
import java.util.Optional;

/**
 * Port secondaire de persistance des insights d'entreprise.
 */
public interface ICompanyInsightRepository {

    void save(CompanyInsight insight);

    Optional<CompanyInsight> findByTickerAndPeriod(String ticker, String formType, String period);

    Optional<CompanyInsight> findLatestByTicker(String ticker);

    List<CompanyInsight> findAllByTicker(String ticker);

    boolean existsByTickerAndPeriod(String ticker, String formType, String period);
}
