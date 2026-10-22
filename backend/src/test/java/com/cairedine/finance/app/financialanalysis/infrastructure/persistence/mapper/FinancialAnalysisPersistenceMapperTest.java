package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.GrowthMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.QualityMetrics;
import com.cairedine.finance.app.financialanalysis.domain.model.ValueMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.GrowthMetricsEmbeddable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FinancialAnalysisPersistenceMapperTest {

    private FinancialAnalysisPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FinancialAnalysisPersistenceMapper();
    }

    @Test
    void shouldMapEntityToDomainCorrectly() {
        // Arrange
        GrowthMetricsEmbeddable growthEmbed = GrowthMetricsEmbeddable.builder()
                .revenueGrowth3Y(new BigDecimal("0.1000"))
                .ebitdaGrowth3Y(new BigDecimal("0.1200"))
                .epsGrowth3Y(new BigDecimal("0.1500"))
                .fcfGrowth(new BigDecimal("0.0800"))
                .build();

        FinancialAnalysisJpaEntity entity = FinancialAnalysisJpaEntity.builder()
                .ticker("AAPL")
                .fiscalYearEndDate("2023-12-31")
                .marketDataAsOf(Instant.now())
                .growthMetrics(growthEmbed)
                .build();

        // Act
        FullMetrics domain = mapper.toDomain(entity);

        // Assert
        assertNotNull(domain);
        assertEquals("2023-12-31", domain.fiscalYearEndDate());
        assertEquals(new BigDecimal("0.1000"), domain.growth().revenueGrowth3Y());
        assertEquals(new BigDecimal("0.1200"), domain.growth().ebitdaGrowth3Y());
        assertEquals(new BigDecimal("0.1500"), domain.growth().epsGrowth3Y());
    }

    @Test
    void shouldMapDomainToEntityCorrectly() {
        // Arrange
        GrowthMetrics growth = new GrowthMetrics(
                new BigDecimal("0.10"), new BigDecimal("0.12"), 
                new BigDecimal("0.15"), new BigDecimal("0.08")
        );
        ValueMetrics value = new ValueMetrics(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        QualityMetrics quality = new QualityMetrics(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        
        FullMetrics domain = new FullMetrics(growth, value, quality, "2023-12-31", Instant.now());

        // Act
        FinancialAnalysisJpaEntity entity = mapper.toEntity("AAPL", domain);

        // Assert
        assertNotNull(entity);
        assertEquals("AAPL", entity.getTicker());
        assertEquals("2023-12-31", entity.getFiscalYearEndDate());
        assertEquals(new BigDecimal("0.10"), entity.getGrowthMetrics().getRevenueGrowth3Y());
        assertEquals(new BigDecimal("0.12"), entity.getGrowthMetrics().getEbitdaGrowth3Y());
        assertNotNull(entity.getLastUpdated());
    }

    @Test
    void shouldReturnNullWhenMappingNulls() {
        assertNull(mapper.toDomain(null));
        
        // Pour toEntity, le mapper crée une entité mais les sous-composants peuvent être nuls
        FullMetrics nullMetrics = new FullMetrics(null, null, null, "2023", null);
        FinancialAnalysisJpaEntity entity = mapper.toEntity("T", nullMetrics);
        
        assertNull(entity.getGrowthMetrics());
        assertNull(entity.getValueMetrics());
        assertNull(entity.getQualityMetrics());
    }
}