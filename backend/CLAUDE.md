# CLAUDE.md — Backend FinanceApp

Instructions pour Claude Code dans `backend/`. Le détail complet de l'architecture est dans [AGENT.md](./AGENT.md). En cas de conflit, le `pom.xml` fait foi pour les versions.

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
- `intelligence/` : module présent dans le code (`domain`, `application`, `infrastructure/sec`, `infrastructure/event`, `infrastructure/web`). Il n'est pas décrit dans `AGENT.md`, vérifie son contenu avant de le modifier.
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

## Ne pas faire

- Ne pas ajouter WebFlux, Mono ou Flux.
- Ne pas exposer une entité JPA dans un contrôleur ou un service de domaine.
- Ne pas modifier le `pom.xml` (versions du BOM) sans demande explicite.
