package com.cairedine.finance.app.watchlist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité JPA pour la persistance de la watchlist.
 * Représente l'association entre un utilisateur et un ticker suivi.
 * La colonne keycloak_id est une clé étrangère vers la table app_user.
 */
@Entity
@Table(name = "user_watchlist", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"keycloak_id", "ticker"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WatchlistItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "keycloak_id", nullable = false)
    private String keycloakId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        if (this.addedAt == null) {
            this.addedAt = Instant.now();
        }
    }
}

