# CLAUDE.md — Backend FinanceApp

Instructions pour Claude Code dans `backend/`. En cas de conflit avec la doc, le `pom.xml` et le code font foi.

## Stack (source : `pom.xml`)

- **Java 25** avec `--enable-preview` (compilateur et `source`/`target` à 25)
- **Spring Boot 4.1.1** (parent `spring-boot-starter-parent`)
- **Spring Modulith 2.1.1** (BOM `spring-modulith-bom`)
- **Spring AI 2.0.1** : `spring-ai-starter-model-google-genai`, `spring-ai-tika-document-reader`
- **Spring Security** + OAuth2 Resource Server (JWT Keycloak)
- **Spring Data JPA** + PostgreSQL (runtime), H2 uniquement en test
- **RestClient** synchrone (`spring-boot-starter-restclient`), pas de WebFlux
- **Caffeine** (cache L1) via `spring-boot-starter-cache`
- **SpringDoc OpenAPI** 3.0.3 (`springdoc-openapi-starter-webmvc-ui`)
- **Actuator** (`/actuator/health`, utilisé par le healthcheck Docker)
- **Lombok** (`@RequiredArgsConstructor`, etc.)
- **Tests** : `spring-boot-starter-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, `spring-security-test`, `spring-modulith-starter-test`, ArchUnit 1.3.0, H2
- Groupe Maven `com.cairedine`, artefact `finance.app`, package racine `com.cairedine.finance.app`

Commandes : `./mvnw test` pour les tests (le groupe `live` est exclu par défaut via `surefire.excludedGroups`), `./mvnw spring-boot:run` pour lancer l'application.

## Structure (Spring Modulith + hexagonale)

Modules sous `src/main/java/com/cairedine/finance/app/` :

- `user/` : auth et synchronisation utilisateur Keycloak. `UserContext` et `UserSyncedEvent` sont publics à la racine du module.
- `financialanalysis/` : calculs de métriques (Growth, Value, Quality), `FinancialMath` dans `domain`.
- `watchlist/` : watchlist utilisateur, écoute l'événement de synchronisation utilisateur.
- `intelligence/` : module présent dans le code (`domain`, `application`, `infrastructure/sec`, `infrastructure/event`, `infrastructure/web`). Son contrôleur est vide pour l'instant, vérifie son contenu avant de le modifier.
- `webclient/` : client externe FMP. Le port `IMarketDataPort` est à la racine, l'implémentation est dans `internal/`.
- `shared/` : `GlobalExceptionHandler`, exceptions de base, configuration transverse.

Chaque module suit la séparation `domain` (logique pure) / `application` (orchestration, services) / `infrastructure` (JPA, web, adaptateurs externes).

## Règles de code

1. **Domain pur** : le package `domain` ne dépend ni de Spring, ni de JPA, ni des annotations web.
2. **Encapsulation Modulith** : seuls les contrats inter-modules sont `public` à la racine du module. Les implémentations restent package-private quand c'est possible. Vérifie avec `ApplicationModules.of(FinanceApp.class).verify()`.
3. **Records** : tous les DTOs, réponses REST et value objects du domaine sont des `record` Java. Les records de réponse portent `@Schema(description = "...")`.
4. **Pas d'entités JPA exposées** : `*JpaEntity` reste dans `infrastructure.persistence`. Les conversions passent par des mappers dédiés (`*PersistenceMapper`, `*WebMapper`).
5. **Injection par constructeur** : `@RequiredArgsConstructor` sur des champs `private final`. Pas de `@Autowired` sur les champs.
6. **Transactions** : `@Transactional(readOnly = true)` sur les lectures du service applicatif. `@Transactional` simple uniquement pour les écritures.
7. **Utilisateur courant** : lire le `UserContext` depuis les details du `JwtAuthenticationToken`. Ne jamais passer d'identifiant utilisateur dans l'URL pour une ressource personnelle (`/api/v1/watchlist`).
8. **Erreurs** : exceptions runtime héritant des bases de `shared.exceptions`, mappées en RFC 7807 `ProblemDetail` par `GlobalExceptionHandler` (`@RestControllerAdvice`).
9. **HTTP externe** : `RestClient` synchrone, optimisé pour les Virtual Threads (`spring.threads.virtual.enabled: true`).
10. **Tests** : `@WebMvcTest` avec `@MockitoBean`, `@DataJpaTest` avec H2, et vérification des modules (`modules.verify()`). Pour les tests de sécurité, utiliser `.with(jwt())`.

## Sécurité

- Authentification stateless par JWT bearer, issuer `http://localhost:8080/realms/dev-realm`.
- `KeycloakJwtConverter` synchronise l'utilisateur en base via `IUserSyncService`, construit le `UserContext` et mappe les rôles `PREMIUM` / `FREEMIUM` en `ROLE_PREMIUM` / `ROLE_FREEMIUM`.
- CORS configuré via `app.cors.allowed-origins` (`http://localhost:4200`), `allowCredentials: true`.

## API REST (vérifiée dans les contrôleurs)

| Méthode | Endpoint | Contrôleur | Réponse |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | `UserController` | `UserContext` |
| `GET` | `/api/v1/company-profile/{ticker}` | `CompanyProfileController` | `CompanyProfileResponse` |
| `GET` | `/api/v1/analysis/{ticker}` | `FinancialAnalysisController` | `List<FullMetricsResponse>` |
| `GET` | `/api/v1/watchlist` | `WatchlistController` | `WatchlistResponse` |
| `POST` | `/api/v1/watchlist/{ticker}` | `WatchlistController` | `WatchlistResponse` (201) |
| `DELETE` | `/api/v1/watchlist/{ticker}` | `WatchlistController` | `WatchlistResponse` |
| `GET` | `/api/v1/watchlist/{ticker}/exists` | `WatchlistController` | `TickerExistsResponse` |

`/api/v1/insights` existe (`InsightController`) mais n'a aucun endpoint pour l'instant.

Toutes les routes sont authentifiées, sauf `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**`, `/error` et les requêtes `OPTIONS`.

## Gestion des erreurs (`GlobalExceptionHandler`, RFC 7807)

| Exception | HTTP | Code |
| :--- | :--- | :--- |
| `TickerNotFoundException` | 404 | `TICKER_NOT_FOUND` |
| `TickerNotInWatchlistException` | 404 | `TICKER_NOT_IN_WATCHLIST` |
| `MarketDataUnavailableException` | 503 | `MARKET_DATA_UNAVAILABLE` |
| `TickerAlreadyInWatchlistException` | 409 | `TICKER_ALREADY_IN_WATCHLIST` |
| `InvalidTickerException` | 400 | `INVALID_TICKER` |
| `WatchlistException` | 400 | `WATCHLIST_ERROR` |
| `ConstraintViolationException` | 400 | `VALIDATION_ERROR` |
| `AccessDeniedException` | 403 | `ACCESS_DENIED` |
| `AuthenticationException` | 401 | `UNAUTHENTICATED` |

## Métriques financières (`financialanalysis`)

- Les métriques sont calculées par `FinancialAnalysisServiceImpl`, avec `FinancialMath` dans `domain`. Les modèles sont `GrowthMetrics`, `ValueMetrics`, `QualityMetrics`, regroupés dans `FullMetrics`.
- **Deux ancres de temps** :
  - Growth et Quality utilisent `fiscalYearEndDate`, la date du dernier rapport publié. Elles restent figées jusqu'au prochain rapport.
  - Value et TTM utilisent `marketDataAsOf` (`Instant.now()`), avec les données de marché et les 12 derniers mois.
- **Cache à deux niveaux** :
  1. `@Cacheable("analysis-metrics")` sur le service, clé = ticker en majuscules. Caffeine, 500 entrées max, expiration après 1h (`application.yaml`).
  2. La table JPA garde l'historique. Elle est interrogée via `metricsCache.findByTickerAndFiscalYear`, avec `fiscalYearEndDate` comme clé. Cela évite de refaire les calculs lorsque le rapport n'a pas changé.
- Les calculs sont parallélisés avec des Virtual Threads.

## Flux d'authentification côté backend

1. Spring Security valide le JWT via les clés JWKS de Keycloak, avec vérification de `iss` et `exp`.
2. `KeycloakJwtConverter` extrait `sub`, `email`, `preferred_username` et `realm_access.roles`.
3. `IUserSyncService.syncAndBuildContext` crée ou met à jour l'utilisateur en base, puis construit le `UserContext`.
4. Les rôles deviennent des authorities `ROLE_<NOM>`, et le `UserContext` est placé dans les details du `JwtAuthenticationToken`.

## Configuration

- `application.yaml` (local) : backend sur le port 8081, PostgreSQL sur `localhost:5434`, base `financeapp`. Pool Hikari à 20 connexions.
- `application-docker.yaml` (profil `docker`) : base `app-db:5432`. JWKS via `http://keycloak:8080`. `FMP_API_KEY` et `GEMINI_API_KEY` sont obligatoires, sans valeur par défaut.
- Clés : `FMP_API_KEY` (client FMP), `GEMINI_API_KEY` (Spring AI, modèle `gemini-2.0-flash`).
- Module `intelligence` : chunking SEC EDGAR (taille 512, overlap 64), user-agent et TTL des filings (90 jours) dans `application.yaml`.

## Points à corriger ou à clarifier

- **Accès PREMIUM non appliqué** : aucun contrôle de rôle sur `/api/v1/analysis`, alors que l'ancienne doc le prévoit. À implémenter ou à retirer de la spécification.
- **Realm Keycloak** : `dev-realm` pour l'issuer, `finance-app` pour la redirection Swagger. À harmoniser.
- **Test d'architecture Modulith** : seul `ArchitectureRulesTest` existe. Le test `Documenter` (génération PlantUML) n'est pas présent.

## Ne pas faire

- Ne pas ajouter WebFlux, Mono ou Flux.
- Ne pas exposer une entité JPA dans un contrôleur ou un service de domaine.
- Ne pas modifier le `pom.xml` (versions du BOM) sans demande explicite.
