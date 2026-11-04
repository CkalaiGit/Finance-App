package com.cairedine.finance.app.intelligence.infrastructure.persistence;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class InsightPersistenceMapper {

    CompanyInsight toDomain(FinancialInsightJpaEntity entity) {
        return new CompanyInsight(
                entity.getTicker(),
                entity.getFormType(),
                entity.getPeriod(),
                entity.getAccessionNumber(),
                entity.getSynthesePerformance(),
                entity.getAnalyseMargesEtDette(),
                entity.getRisquesPrincipaux(),
                entity.getGuidanceManagement(),
                entity.getChaineApprovisionnement(),
                entity.getFaitsMarquants(),
                entity.getGeneratedAt()
        );
    }

    void updateEntity(
            FinancialInsightJpaEntity entity,
            String ticker,
            CompanyInsight insight,
            Instant generatedAt) {
        entity.setTicker(ticker);
        entity.setFormType(insight.formType());
        entity.setPeriod(insight.period());
        entity.setAccessionNumber(insight.accessionNumber());
        entity.setSynthesePerformance(insight.synthesePerformance());
        entity.setAnalyseMargesEtDette(insight.analyseMargesEtDette());
        entity.setRisquesPrincipaux(insight.risquesPrincipaux());
        entity.setGuidanceManagement(insight.guidanceManagement());
        entity.setChaineApprovisionnement(insight.chaineApprovisionnement());
        entity.setFaitsMarquants(insight.faitsMarquants());
        entity.setGeneratedAt(generatedAt);
    }
}
