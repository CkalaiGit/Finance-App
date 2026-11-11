# FinanceHub — Backend Spring Boot

API REST de FinanceHub : données de marché (FMP), filings SEC, analyses financières, watchlist et module IA. Spring Boot 4, Java 25 (preview), PostgreSQL, OAuth2 Resource Server avec Keycloak.

## Démarrage

Pour démarrer l'ensemble du projet (Keycloak, bases de données, frontend), suivre le guide de démarrage à la racine du monorepo : [README.md](../README.md)

Lancement local, avec Keycloak et `app-db` démarrés par Docker Compose :

```bash
export FMP_API_KEY=votre_cle_fmp
./mvnw spring-boot:run
```

Le module IA démarre par défaut en mode `stub`, sans clé Gemini ni appel réseau.
Pour une synthèse réelle, définir `INTELLIGENCE_AI_MODE=gemini`, renseigner
`GEMINI_API_KEY` avec une clé Google AI Studio et sélectionner dans `GEMINI_MODEL`
un modèle Gemini disponible dans l'offre gratuite.

L'API écoute sur http://localhost:8081, Swagger UI sur http://localhost:8081/swagger-ui.html.

## Tests

```bash
./mvnw verify
```

## Image Docker

Le `Dockerfile` construit une image multi-étapes (JDK 25 pour la compilation, JRE 25 pour l'exécution) qui lance l'application avec `--enable-preview` et le profil `docker`.

```bash
docker build -t financehub-backend .
```
