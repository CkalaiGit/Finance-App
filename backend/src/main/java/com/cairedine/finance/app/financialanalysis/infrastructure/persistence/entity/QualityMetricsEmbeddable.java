package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.math.BigDecimal;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualityMetricsEmbeddable {
    @Column(precision = 19, scale = 4)
    private BigDecimal roic;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal operatingMargin;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal netDebtToEbitda;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal freeCashFlowMargin;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal sgaToRevenue;
}