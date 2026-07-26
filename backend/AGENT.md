# AGENT.md — Contexte Backend Spring Boot & Architecture System

> **Fichier de contexte pour agent IA et développeurs travaillant sur le backend `backend-financeHub`.**  
> Ce document définit l'architecture système, les choix technologiques, la sécurité Keycloak, les contrats d'API REST, la structure par modules Spring Modulith / Hexagonale et les règles d'ingénierie logicielle du backend.

---

## 1. Vue d'ensemble et Stack Technologique

Le backend **FinanceApp** est une application financière distribuée en architecture hexagonale modulaire (**Spring Modulith**), conçue pour consommer des données de marché en temps réel (FMP), exécuter des calculs d'analyse financière poussés et gérer le profil et la *watchlist* des utilisateurs authentifiés.

### Stack Réelle du projet (`pom.xml`)
- **Langage** : Java 25 (Preview Features activées `--enable-preview`)
- **Framework Core** : **Spring Boot 4.0.6**
- **Architecture Modulaire** : **Spring Modulith 2.0.3**
- **Exécution Concurrente** : **Virtual Threads** (Loom) activés (`spring.threads.virtual.enabled: true`)
- **Sécurité & Authentification** : Spring Security 6 + OAuth2 Resource Server (**Keycloak JWT**)
- **Client HTTP Externe** : **RestClient** synchrone (aligné et optimal pour Virtual Threads, sans WebFlux)
- **Cache L1 (RAM)** : **Caffeine Cache** (TTL 1h, max 500 entrées) avec `@EnableCaching` et `@Cacheable`
- **Base de Données** :
  - Production / Dev : PostgreSQL (`jdbc:postgresql://localhost:5434/financeapp`) avec pool **HikariCP**
  - Tests : Base **H2** en mémoire
- **Documentation API** : **SpringDoc OpenAPI 3.0.1** (`/swagger-ui.html`, `/v3/api-docs`)
- **Outillage Code** : **Lombok**, Annotations **JSpecify** (`@NonNull`)
- **Testing & Quality** :
  - **JUnit 5**, **Mockito** (avec `@MockitoBean`), **AssertJ**
  - **ArchUnit 1.3.0** pour l'invariance architecturale
  - Starters de test dédiés Spring Boot 4 (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, `spring-security-test`)

---

## 2. Architecture de Sécurité Keycloak & Spring Security

### Topology & Configuration Keycloak
- **IdP Keycloak Server** : `http://localhost:8080` (Realm: `dev-realm`)
- **Backend Service Port** : `http://localhost:8081`
- **Frontend Client** : `http://localhost:4200` (Angular 20)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/dev-realm
```

### Conversion JWT & Propagation des Rôles (`KeycloakJwtConverter`)
L'authentification est 100% **stateless** via Token JWT bearer.
Le converter personnalisé [`KeycloakJwtConverter`](file:///c:/dev/Projet-App-Finance/backend-financeHub/src/main/java/com/cairedine/finance/app/user/infrastructure/security/KeycloakJwtConverter.java) intercepte chaque requête authentifiée :
1. Extrait le token JWT émis par Keycloak.
2. Synchronise automatiquement l'utilisateur avec la base locale PostgreSQL via `IUserSyncService`.
3. Construit un objet immuable [`UserContext`](file:///c:/dev/Projet-App-Finance/backend-financeHub/src/main/java/com/cairedine/finance/app/user/UserContext.java).
4. Mappe les rôles (`PREMIUM`, `FREEMIUM`) en authorities Spring Security sous le format `ROLE_<ROLE_NAME>` (ex: `ROLE_PREMIUM`, `ROLE_FREEMIUM`).
5. Stocke le `UserContext` dans les *details* du `JwtAuthenticationToken`.

### Configuration CORS (`WebConfig` & `CorsProperties`)
- Origines autorisées configurable dans `application.yaml` via `app.cors.allowed-origins` (`http://localhost:4200`).
- Méthodes autorisées : `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`.
- Expressément configuré pour supporter l'envoi d'en-têtes de credentials (`allowCredentials: true`).

---

## 3. Mapping des APIs REST Backend

Tous les endpoints respectent les principes RESTful et retournent des réponses HTTP normalisées.

| Méthode | Endpoint | Description | Sécurité / Rôles | DTO de Réponse |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | Récupère le profil et les rôles de l'utilisateur connecté | Authentifié | `UserContext` |
| `GET` | `/api/v1/company-profile/{ticker}` | Récupère les données statiques d'entreprise (description, secteur, prix, mktCap) | Authentifié | `CompanyProfileResponse` |
| `GET` | `/api/v1/analysis/{ticker}` | Calcule et retourne la série temporelle des métriques d'analyse financière (Growth, Value, Quality) | Authentifié (Accès complet soumis au rôle `PREMIUM`) | `List<FullMetricsResponse>` |
| `GET` | `/api/v1/watchlist` | Récupère la liste des tickers surveillés par l'utilisateur connecté | Authentifié | `WatchlistResponse` |
| `POST` | `/api/v1/watchlist/{ticker}` | Ajoute un ticker à la watchlist utilisateur | Authentifié | `WatchlistResponse` (201 Created) |
| `DELETE` | `/api/v1/watchlist/{ticker}` | Supprime un ticker de la watchlist utilisateur | Authentifié | `WatchlistResponse` |
| `GET` | `/api/v1/watchlist/{ticker}/exists` | Vérifie si un ticker est présent dans la watchlist | Authentifié | `TickerExistsResponse` |

### Gestion globale des Exceptions (`GlobalExceptionHandler`)
Le backend utilise la norme **RFC 7807 `ProblemDetail`** pour uniformiser les erreurs API :
- `TickerNotFoundException` (404 Not Found) -> Ticker inconnu ou non supporté.
- `MarketDataUnavailableException` (503 Service Unavailable) -> Indisponibilité du fournisseur FMP.
- `WatchlistException` (400 Bad Request) -> Règle métier violée lors de la manipulation de la watchlist.

---

## 4. Structure du Projet (Spring Modulith + Hexagonale)

Le projet s'appuie sur une **Architecture Hexagonale modulaire** séparée en *Bounded Contexts* via Spring Modulith :

```
com.cairedine.finance.app
├── FinanceApp.java                        # Main Spring Boot Application
├── user/                                  # Module Gestion Utilisateur & Auth
│   ├── UserContext.java                   # [API Publique] Record de contexte utilisateur
│   ├── UserSyncedEvent.java               # [API Publique] Événement de synchronisation user
│   ├── UserController.java                # REST Endpoint /api/users/me
│   ├── application/                       # Services applicatifs (UserSyncServiceImpl)
│   ├── domain/                            # Ports et modèle métier User
│   └── infrastructure/                    # JPA Entities, Repositories, Security & Keycloak Config
├── financialanalysis/                     # Module Calculs & Métriques Financières
│   ├── application/                       # Cas d'usage FinancialAnalysisServiceImpl
│   ├── domain/                            # FinancialMath, métriques métier (Growth, Value, Quality)
│   └── infrastructure/
│       ├── persistence/                   # FinancialAnalysisJpaEntity, Embeddables, Mappers
│       └── web/                           # Controllers REST & DTOs (/api/v1/analysis, /api/v1/company-profile)
├── watchlist/                             # Module Watchlist Utilisateur
│   ├── domain/                            # WatchlistAggregate, WatchlistService
│   ├── infrastructure/                    # WatchlistItemJpaEntity, Event Listener (UserSync)
│   └── web/                               # WatchlistController (/api/v1/watchlist)
├── webclient/                             # Integration Client FMP Externe
│   ├── IMarketDataPort.java               # Port d'accès aux données de marché
│   └── internal/                          # RestClient implementation, FmpProperties & DTOs FMP
└── shared/                                # Transversal (Exceptions, GlobalExceptionHandler)
```

---

## 5. Règles et Conventions de Code Backend

### 1. Immuabilité et DTOs
- **Java Records** : Tous les DTOs REST (`CompanyProfileResponse`, `FullMetricsResponse`, `WatchlistResponse`), contextes et modèles de valeurs de domaine DOIVENT être déclinés sous forme de `record` Java.
- **Documentation Schema** : Décorer les records DTO avec `@Schema(description = "...")` pour générer automatiquement OpenAPI 3.

### 2. Modulith & Masquage de l'Implémentation
- Seuls les contrats d'API inter-modules (ex: `UserContext`, `IMarketDataPort`) doivent résider à la racine du module et être `public`.
- Les services d'implémentation et les adaptateurs d'infrastructure situés dans des sous-packages doivent conserver la visibilité **package-private** lorsque possible afin de faire respecter l'isolation stricte vérifiée par `Spring Modulith`.

### 3. Isolation Hexagonale (Entities vs Domain)
- Ne **JAMAIS** exposer d'entités JPA (`@Entity`) aux contrôleurs REST ni aux services du domaine.
- Séparer strictement les objets de persistance JPA (`DBUserJpaEntity`, `WatchlistItemJpaEntity`) des modèles purs du domaine (`User`, `WatchlistAggregate`).
- Utiliser des mappers dédiés (`*PersistenceMapper`, `*WebMapper`) pour effectuer la transformation entre couches.

### 4. Client HTTP et Virtual Threads
- Privilégier **RestClient** synchrone avec Virtual Threads (`spring.threads.virtual.enabled: true`).
- Éviter d'introduire des dépendances réactives lourdes type WebFlux/Mono/Flux qui complexifieraient inutilement le modèle d'exécution.

### 5. Inversion de Contrôle et Lombok
- Pas de `@Autowired` sur les champs. Utiliser l'injection par constructeur via l'annotation Lombok `@RequiredArgsConstructor` sur des champs déclarés `private final`.

### 6. Transactions & Persistance
- Annoter les méthodes de lecture seule du service applicatif avec `@Transactional(readOnly = true)`.
- Réserver `@Transactional` (écriture) uniquement aux opérations modifiant l'état du domaine.

### 7. Stratégie de Testing JUnit 5 / Spring Boot 4
- **Tests WebMVC** : `@WebMvcTest(TargetController.class)` avec `@MockitoBean` (syntaxe conforme Spring Boot 4) pour simuler les dépendances.
- **Tests JPA** : `@DataJpaTest` adossé à la base H2 en mémoire.
- **Tests de Structure Modulith** : Utiliser `ApplicationModules.of(FinanceApp.class).verify()` pour s'assurer qu'aucun couplage illégal inter-module n'est introduit.
