package com.cairedine.finance.app.intelligence.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "intelligence_insights", uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "form_type", "period"}))
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinancialInsightJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "form_type", nullable = false)
    private String formType;

    @Column(nullable = false)
    private String period;

    @Column(name = "accession_number")
    private String accessionNumber;

    @Column(columnDefinition = "text")
    private String synthesePerformance;

    @Column(columnDefinition = "text")
    private String analyseMargesEtDette;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "text")
    private List<String> risquesPrincipaux;

    @Column(columnDefinition = "text")
    private String guidanceManagement;

    @Column(columnDefinition = "text")
    private String chaineApprovisionnement;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "text")
    private List<String> faitsMarquants;

    @Column
    private Instant generatedAt;

    @Column
    private Instant lastUpdated;

    @Column
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
        if (expiresAt == null && generatedAt != null) {
            expiresAt = generatedAt.plus(90, ChronoUnit.DAYS);
        }
    }
}
