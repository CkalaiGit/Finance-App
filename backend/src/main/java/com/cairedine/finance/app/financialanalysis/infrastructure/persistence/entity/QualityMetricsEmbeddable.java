package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.math.BigDecimal;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualityMetricsEmbeddable {
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal roic;
    
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal operatingMargin;
    
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal netDebtToEbitda;
    
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal freeCashFlowMargin;
    
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal sgaToRevenue;
}