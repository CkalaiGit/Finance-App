package com.cairedine.finance.app.user.infrastructure.persistence.entity;

import com.cairedine.finance.app.financialanalysis.infrastructure.persistence.entity.FinancialAnalysisJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DBUserJpaEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String username;

    @Getter(AccessLevel.NONE)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "app_user_roles",
            joinColumns = @JoinColumn(name = "keycloak_id")
    )
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(
            name = "user_watchlist",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "analysis_id")
    )
    private Set<FinancialAnalysisJpaEntity> watchlist = new HashSet<>();

    public DBUserJpaEntity(String keycloakId, String email, String username, Set<String> roles) {
        this.keycloakId = keycloakId;
        this.email      = email;
        this.username   = username;
        this.roles      = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.createdAt  = Instant.now();
        this.updatedAt  = Instant.now();
    }

    public void updateFrom(String email, String username, Set<String> roles) {
        this.email     = email;
        this.username  = username;
        this.roles     = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.updatedAt = Instant.now();
    }

    public Set<String> getRoles() {
        return Set.copyOf(roles);
    }
}
