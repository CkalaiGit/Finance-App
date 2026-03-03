package com.cairedine.finance.app.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DBUser {

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

    public DBUser(String keycloakId, String email, String username, Set<String> roles) {
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