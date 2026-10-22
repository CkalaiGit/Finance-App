# FinanceHub — Backend Spring Boot

API REST de FinanceHub : données de marché (FMP), filings SEC, analyses financières, watchlist et module IA. Spring Boot 4, Java 25 (preview), PostgreSQL, OAuth2 Resource Server avec Keycloak.

## Démarrage

Pour démarrer l'ensemble du projet (Keycloak, bases de données, frontend), suivre le guide du dépôt d'infrastructure : https://github.com/CkalaiGit/Infra-KC_projet_finance#readme

Lancement local, avec Keycloak et `app-db` démarrés par Docker Compose :

```bash
export FMP_API_KEY=votre_cle_fmp
export GEMINI_API_KEY=votre_cle_gemini
./mvnw spring-boot:run
```

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
