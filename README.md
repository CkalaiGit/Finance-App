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
3. Renseigner `FMP_API_KEY` dans `.env` pour les données de marché (clé sur https://site.financialmodelingprep.com/developer/docs).
   Le module IA fonctionne par défaut en mode `stub`, sans clé Gemini ni appel réseau.
   Pour activer Gemini, définir `INTELLIGENCE_AI_MODE=gemini`, renseigner une clé Google AI Studio (`GEMINI_API_KEY`, https://aistudio.google.com/apikey) et choisir dans `GEMINI_MODEL` un modèle disponible dans l'offre gratuite de Google.
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
