# Backend Antigravity Rules — Finance App Backend

Refer to [AGENT.md](file:///c:/dev/Projet-App-Finance/backend-financeHub/AGENT.md) and [.antigravity/rules.md](file:///c:/dev/Projet-App-Finance/backend-financeHub/.antigravity/rules.md) for full architectural guidelines.

## Quick Rules Summary:
- Java 25 + Spring Boot 4.0.6 + Spring Modulith 2.0.3.
- Use Java `record` for all DTOs and value objects.
- Keep JPA entities isolated inside `infrastructure.persistence`. Never expose entities in REST controllers.
- Use `RestClient` (synchronous) for external API integration (FMP), leveraging Java Virtual Threads.
- Inject dependencies via `@RequiredArgsConstructor` on private final fields.
- Errors must use RFC 7807 `ProblemDetail` via `@RestControllerAdvice` (`GlobalExceptionHandler`).
