# FinanceHub

Application d'analyse financière : frontend Angular, API Spring Boot, authentification Keycloak.

## Démarrage

1. Cloner le dépôt :
   ```bash
   git clone <url-du-monorepo> financehub
   cd financehub
   ```
2. Créer le fichier de configuration :
   ```bash
   cp .env.example .env
   ```
3. Renseigner les clés d'API dans `.env` :
   - `FMP_API_KEY` : données de marché, clé sur https://site.financialmodelingprep.com/developer/docs
   - `GEMINI_API_KEY` : module IA, clé sur https://aistudio.google.com/apikey
4. Lancer l'application :
   ```bash
   docker compose up --build
   ```

## URLs

| Service | URL |
|---|---|
| Application Angular | http://localhost:4200 |
| API Spring Boot | http://localhost:8081 |
| Console Keycloak | http://localhost:8080 |
| app-db (PostgreSQL) | localhost:5434 |

## Comptes de démonstration

| Identifiant | Mot de passe |
|---|---|
| `warren` | `password` |
| `michael` | `password` |
| `admin` | `admin` (console Keycloak) |

> **Avertissement** : identifiants de développement uniquement. Ne jamais les utiliser en production.
