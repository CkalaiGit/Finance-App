package com.cairedine.finance.app.financialanalysis.infrastructure.persistence.adapter;

import com.cairedine.finance.app.financialanalysis.domain.model.FullMetrics;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.mapper.FinancialAnalysisPersistenceMapper;
import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.repository.IFinancialAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsCachePersistenceAdapterTest {

    @Mock
    private IFinancialAnalysisRepository repository;

    @Mock
    private FinancialAnalysisPersistenceMapper mapper;

    @InjectMocks
    private MetricsCachePersistenceAdapter adapter;

    private static final String TICKER = "MSFT";
    private static final String FISCAL_YEAR = "2023-12-31";

    // ==================== findAllByTicker Tests ====================

    @Test
    @DisplayName("1. findAllByTicker doit retourner les entités non-expirées")
    void shouldFindAllByTickerExcludingExpiredEntities() {
        // Arrange
        FinancialAnalysisJpaEntity nonExpiredEntity = new FinancialAnalysisJpaEntity();
        nonExpiredEntity.setExpiresAt(Instant.now().plus(Duration.ofHours(12)));

        FinancialAnalysisJpaEntity expiredEntity = new FinancialAnalysisJpaEntity();
        expiredEntity.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));

        FullMetrics domainMetrics = mock(FullMetrics.class);

        when(repository.findAllByTickerOrderByFiscalYearEndDateDesc(TICKER))
                .thenReturn(List.of(nonExpiredEntity, expiredEntity));
        when(mapper.toDomain(nonExpiredEntity)).thenReturn(domainMetrics);

        // Act
        List<FullMetrics> result = adapter.findAllByTicker(TICKER);

        // Assert
        assertEquals(1, result.size());
        assertEquals(domainMetrics, result.getFirst());
        verify(mapper).toDomain(nonExpiredEntity);
        verify(mapper, never()).toDomain(expiredEntity);
    }

    @Test
    @DisplayName("2. findAllByTicker doit retourner une liste vide si toutes les entités sont expirées")
    void shouldReturnEmptyListWhenAllEntitiesExpired() {
        // Arrange
        FinancialAnalysisJpaEntity expiredEntity1 = new FinancialAnalysisJpaEntity();
        expiredEntity1.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));

        FinancialAnalysisJpaEntity expiredEntity2 = new FinancialAnalysisJpaEntity();
        expiredEntity2.setExpiresAt(Instant.now().minus(Duration.ofHours(2)));

        when(repository.findAllByTickerOrderByFiscalYearEndDateDesc(TICKER))
                .thenReturn(List.of(expiredEntity1, expiredEntity2));

        // Act
        List<FullMetrics> result = adapter.findAllByTicker(TICKER);

        // Assert
        assertTrue(result.isEmpty());
        verify(mapper, never()).toDomain(any());
    }

    // ==================== findByTickerAndFiscalYear Tests ====================

    @Test
    @DisplayName("3. findByTickerAndFiscalYear doit retourner Optional.empty() si expirée")
    void shouldReturnEmptyIfEntityExpired() {
        // Arrange
        FinancialAnalysisJpaEntity expiredEntity = new FinancialAnalysisJpaEntity();
        expiredEntity.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.of(expiredEntity));

        // Act
        Optional<FullMetrics> result = adapter.findByTickerAndFiscalYear(TICKER, FISCAL_YEAR);

        // Assert
        assertTrue(result.isEmpty());
        verify(mapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("4. findByTickerAndFiscalYear doit retourner les données si valides")
    void shouldReturnMetricsIfNotExpired() {
        // Arrange
        FinancialAnalysisJpaEntity validEntity = new FinancialAnalysisJpaEntity();
        validEntity.setExpiresAt(Instant.now().plus(Duration.ofHours(12)));

        FullMetrics domainMetrics = mock(FullMetrics.class);

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.of(validEntity));
        when(mapper.toDomain(validEntity)).thenReturn(domainMetrics);

        // Act
        Optional<FullMetrics> result = adapter.findByTickerAndFiscalYear(TICKER, FISCAL_YEAR);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(domainMetrics, result.get());
        verify(mapper).toDomain(validEntity);
    }

    @Test
    @DisplayName("5. findByTickerAndFiscalYear doit retourner Optional.empty() si non trouvée")
    void shouldReturnEmptyIfNotFound() {
        // Arrange
        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.empty());

        // Act
        Optional<FullMetrics> result = adapter.findByTickerAndFiscalYear(TICKER, FISCAL_YEAR);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== save Tests ====================

    @Test
    @DisplayName("6. save doit ignorer si l'entité existe et n'est pas expirée")
    void shouldNotSaveIfDataExistsAndNotExpired() {
        // Arrange
        FullMetrics metrics = mock(FullMetrics.class);
        when(metrics.fiscalYearEndDate()).thenReturn(FISCAL_YEAR);

        FinancialAnalysisJpaEntity existingEntity = new FinancialAnalysisJpaEntity();
        existingEntity.setExpiresAt(Instant.now().plus(Duration.ofDays(5)));

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.of(existingEntity));

        // Act
        adapter.save(TICKER, metrics);

        // Assert
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("7. save doit supprimer et recréer si l'entité est expirée")
    void shouldDeleteAndSaveIfEntityExpired() {
        // Arrange
        FullMetrics metrics = mock(FullMetrics.class);
        when(metrics.fiscalYearEndDate()).thenReturn(FISCAL_YEAR);

        FinancialAnalysisJpaEntity expiredEntity = new FinancialAnalysisJpaEntity();
        expiredEntity.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));

        FinancialAnalysisJpaEntity newEntity = new FinancialAnalysisJpaEntity();

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.of(expiredEntity));
        when(mapper.toEntity(TICKER, metrics)).thenReturn(newEntity);

        // Act
        adapter.save(TICKER, metrics);

        // Assert
        verify(repository).delete(expiredEntity);
        verify(repository).save(newEntity);
    }

    @Test
    @DisplayName("8. save doit créer une nouvelle entité si n'existe pas")
    void shouldSaveNewEntityIfDoesNotExist() {
        // Arrange
        FullMetrics metrics = mock(FullMetrics.class);
        when(metrics.fiscalYearEndDate()).thenReturn(FISCAL_YEAR);

        FinancialAnalysisJpaEntity newEntity = new FinancialAnalysisJpaEntity();

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.empty());
        when(mapper.toEntity(TICKER, metrics)).thenReturn(newEntity);

        // Act
        adapter.save(TICKER, metrics);

        // Assert
        verify(repository).save(newEntity);
        verify(repository, never()).delete(any());
    }

    // ==================== TTL Tests ====================

    @Test
    @DisplayName("9. save doit définir expiresAt à now() + 24 heures")
    void shouldSetExpirationTo24Hours() {
        // Arrange
        FullMetrics metrics = mock(FullMetrics.class);
        when(metrics.fiscalYearEndDate()).thenReturn(FISCAL_YEAR);

        FinancialAnalysisJpaEntity newEntity = new FinancialAnalysisJpaEntity();

        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.empty());
        when(mapper.toEntity(TICKER, metrics)).thenReturn(newEntity);

        Instant beforeSave = Instant.now();

        // Act
        adapter.save(TICKER, metrics);

        Instant afterSave = Instant.now();

        // Assert
        assertNotNull(newEntity.getExpiresAt());

        // Vérifier que expiresAt est approximativement 24h après now()
        Instant expectedMin = beforeSave.plus(Duration.ofHours(24)).minus(Duration.ofSeconds(1));
        Instant expectedMax = afterSave.plus(Duration.ofHours(24)).plus(Duration.ofSeconds(1));

        assertTrue(newEntity.getExpiresAt().isAfter(expectedMin));
        assertTrue(newEntity.getExpiresAt().isBefore(expectedMax));

        verify(repository).save(newEntity);
    }

    // ==================== isExpired() Entity Tests ====================

    @Test
    @DisplayName("10. isExpired() doit retourner true pour une entité expirée")
    void shouldReturnTrueIfExpired() {
        // Arrange
        FinancialAnalysisJpaEntity entity = new FinancialAnalysisJpaEntity();
        entity.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));

        // Act & Assert
        assertTrue(entity.isExpired());
    }

    @Test
    @DisplayName("11. isExpired() doit retourner false pour une entité valide")
    void shouldReturnFalseIfNotExpired() {
        // Arrange
        FinancialAnalysisJpaEntity entity = new FinancialAnalysisJpaEntity();
        entity.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        // Act & Assert
        assertFalse(entity.isExpired());
    }

    @Test
    @DisplayName("12. isExpired() doit retourner false si expiresAt est null")
    void shouldReturnFalseIfExpiresAtIsNull() {
        // Arrange
        FinancialAnalysisJpaEntity entity = new FinancialAnalysisJpaEntity();
        entity.setExpiresAt(null);

        // Act & Assert
        assertFalse(entity.isExpired());
    }

    @Test
    @DisplayName("13. shouldDelegateFindByTickerAndFiscalYear")
    void shouldDelegateFindByTickerAndFiscalYear() {
        // Arrange
        when(repository.findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR))
                .thenReturn(Optional.empty());

        // Act
        adapter.findByTickerAndFiscalYear(TICKER, FISCAL_YEAR);

        // Assert
        verify(repository).findByTickerAndFiscalYearEndDate(TICKER, FISCAL_YEAR);
    }
}