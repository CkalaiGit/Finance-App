# Backend Development Rules — Spring Boot & Hexagonal Architecture

## Architectural Principles
1. **Hexagonal Architecture Discipline**:
   - The `domain` package contains pure Java logic and value objects. It MUST NOT depend on Spring, JPA, or web annotations.
   - The `application` package orchestrates domain services and ports implementation.
   - The `infrastructure` package manages controllers, database entities (`*JpaEntity`), and external HTTP adapters.

2. **Spring Modulith Encapsulation**:
   - Keep internal components package-private inside `application` or `infrastructure` subpackages.
   - Only expose public records/events at the root of a module (e.g. `com.cairedine.finance.app.user.UserContext`).

3. **Immutability First**:
   - Use Java `record` for all DTOs, Web Responses, and Domain Value Objects.
   - Decorate REST response records with `@Schema` for OpenAPI documentation.

4. **Security & User Context**:
   - Retrieve authenticated user details from `JwtAuthenticationToken` details (`UserContext`).
   - Never accept user IDs as route parameters when operating on user-specific resources (`/api/v1/watchlist`).

5. **Concurrency & HTTP Clients**:
   - Use Spring 6 / Boot 4 `RestClient` for synchronous HTTP integrations (optimised for Java 25 Virtual Threads).

6. **Error Handling**:
   - Throw domain-specific runtime exceptions extending base exceptions in `shared.exceptions`.
   - Map domain exceptions to standard RFC 7807 `ProblemDetail` responses in `GlobalExceptionHandler`.

7. **Testing Standards**:
   - Use `@WebMvcTest` for web layer tests with `@MockitoBean`.
   - Use `@DataJpaTest` for persistence layer tests with H2 database.
   - Include ArchUnit / Modulith verifications (`modules.verify()`) to enforce module boundaries.
