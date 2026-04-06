package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValueMetricsEmbeddable {
    @Column(precision = 19, scale = 4)
    private BigDecimal evToEbit;
    @Column(precision = 19, scale = 4)
    private BigDecimal peRatioTTM;
    @Column(precision = 19, scale = 4)
    private BigDecimal pegRatioForward;
    @Column(precision = 19, scale = 4)
    private BigDecimal evToSales;
}
