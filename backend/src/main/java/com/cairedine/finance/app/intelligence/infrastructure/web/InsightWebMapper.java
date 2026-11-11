package com.cairedine.finance.app.intelligence.infrastructure.web;

import com.cairedine.finance.app.intelligence.domain.model.CompanyInsight;
import com.cairedine.finance.app.intelligence.domain.model.IngestionStatus;
import com.cairedine.finance.app.intelligence.infrastructure.web.dto.IngestionStatusResponse;
import com.cairedine.finance.app.intelligence.infrastructure.web.dto.InsightResponse;
import org.springframework.stereotype.Component;

@Component
class InsightWebMapper {

    InsightResponse toResponse(CompanyInsight insight) {
        return new InsightResponse(
                insight.ticker(),
                insight.formType(),
                insight.period(),
                insight.accessionNumber(),
                insight.synthesePerformance(),
                insight.analyseMargesEtDette(),
                insight.risquesPrincipaux(),
                insight.guidanceManagement(),
                insight.chaineApprovisionnement(),
                insight.faitsMarquants(),
                insight.generatedAt()
        );
    }

    IngestionStatusResponse toResponse(IngestionStatus status) {
        return new IngestionStatusResponse(
                status.ticker(),
                status.ready(),
                status.inProgress(),
                status.count(),
                status.lastGenerated()
        );
    }
}
