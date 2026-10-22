# FinanceApp — Agent Context & Architecture Frontend

## Projet
Application d'analyse financière Angular 20 (standalone components, signals-ready) avec authentification Keycloak.
Permet aux utilisateurs (PREMIUM / FREEMIUM) de consulter des métriques financières de sociétés cotées (croissance, valorisation, qualité) et de gérer une liste de suivi (Watchlist) personnalisée.

---

## Stack Technique
| Couche | Technologie |
|--------|-------------|
| Frontend | Angular 20 (standalone components, `ChangeDetectionStrategy.OnPush`) |
| Auth | Keycloak 26 (PKCE S256, check-sso silencieux via iframe) |
| RxJS | Parallélisation `forkJoin`, `BehaviorSubject` pour l'état d'authentification |
| CSS | SCSS + variables CSS custom (design system fintech sombre avec glassmorphism) |
| Tests | Vitest + happy-dom (`npm test`) |
| Backend | Spring Boot 4 sur `http://localhost:8081` |
| IdP | Keycloak sur `http://localhost:8080`, realm `dev-realm` |

---

## Architecture Auth (CRITIQUE — ne jamais contourner)
```
KeycloakService       → Wrapper bas niveau, contient l'instance Keycloak-js
AuthService           → Logique métier (isLoggedIn, getUserProfile, login, logout)
AuthStoreService      → State management RxJS (BehaviorSubject<UserProfile|null>)
authInterceptor       → Ajoute automatiquement Bearer token sur chaque requête HTTP
authGuard             → Protège les routes, redirige vers Keycloak si non connecté
```

---

## Rôles Utilisateurs & Comptes Dev (realm-dev.json)
- `PREMIUM` — accès complet aux analyses et à la watchlist
- `FREEMIUM` — accès limité aux données publiques

| Username | Password | Rôle |
|----------|----------|------|
| warren | password | PREMIUM |
| michael | password | FREEMIUM |

---

## Routes Frontend
```
/home            → LandingPageComponent (publique, présentation et recherche)
/details/:ticker → CompanyDetailsComponent (publique, profil entreprise & analyses)
/watchlist       → WatchlistComponent (protégée par authGuard, tableau tabulé de suivi)
/about           → À implémenter
```

---

## API Backend Consommée
Base URL : `http://localhost:8081`

| Méthode | Endpoint | Description | Sécurité |
|---|---|---|---|
| `GET` | `/api/v1/companies/:ticker` | Fiche synthétique d'une entreprise | Publique / Authentifié |
| `GET` | `/api/v1/company-profile/:ticker` | Profil complet (logo, secteur, prix, cap) | Authentifié |
| `GET` | `/api/v1/analysis/:ticker` | Métriques financières (Growth, Value, Quality) | Authentifié (PREMIUM) |
| `GET` | `/api/v1/users/me` | Context utilisateur (`UserContext`) | Authentifié |
| `GET` | `/api/v1/watchlist` | Récupère la watchlist de l'utilisateur | Authentifié |
| `POST` | `/api/v1/watchlist/:ticker` | Ajoute un ticker à la watchlist | Authentifié |
| `DELETE` | `/api/v1/watchlist/:ticker` | Supprime un ticker de la watchlist | Authentifié |
| `GET` | `/api/v1/watchlist/:ticker/exists` | Vérifie si un ticker est dans la watchlist | Authentifié |

---

## Modèles TypeScript Clés

```typescript
export interface UserProfile {
   username: string;
   email: string;
   roles: string[];
}

export interface WatchlistResponse {
   keycloakId: string;
   tickers: string[];
}

export interface TickerExistsResponse {
   ticker: string;
   exists: boolean;
}

export interface FullMetrics {
   growth: GrowthMetrics;
   value: ValueMetrics;
   quality: QualityMetrics;
   fiscalYearEndDate: string | null;
   marketDataAsOf: string | null;
}

export interface CompanyProfileResponse {
   symbol: string;
   companyName: string;
   industry: string;
   image: string;
   beta: number;
   marketCap: number;
   price: number;
   description: string;
}
```

---

## Composants & Services Clés

### Services (`src/app/services/`)
- `WatchlistService` : Gère le CRUD de la watchlist utilisateur via HTTP.
- `FinancialAnalysisService` : Récupère les métriques `FullMetrics` d'un ticker.
- `CompanyService` : Récupère le profil entreprise et la liste des sociétés.
- `AuthStoreService` : Expose `user$` et `isLoggedIn$` de manière réactive.

### Composants Pages (`src/app/components/pages/`)
- `WatchlistComponent` :
  - Formulaire d'ajout inline (auto-uppercase, validation anti-doublon).
  - Tableau à 3 onglets (Croissance, Valorisation, Qualité) reprenant le design system de `CompanyDetailsComponent`.
  - Appels de métriques parallélisés avec `forkJoin`.
  - Suppression optimiste (`bi-trash` avec `stopPropagation`).
  - Gestion des états : chargement (`isLoading`), vide (`empty-state` avec CTA vers `/home`), erreur réseau contextuelle (`errorMessage` ou `—` dans les cellules).
- `CompanyDetailsComponent` :
  - Visualisation des métriques financières.
  - Bouton toggle dynamic d'ajout/retrait de la Watchlist (`bi-star` vs `bi-star-fill`).

---

## Design System — Variables CSS (`styles.scss`)
```scss
// Backgrounds
--bg-primary, --bg-secondary, --bg-card, --bg-card-hover

// Texte
--text-primary, --text-secondary, --text-muted

// Accents
--accent-blue (#3b82f6), --accent-purple (#8b5cf6),
--accent-cyan (#06b6d4), --accent-green (#10b981), --accent-red (#ef4444), --accent-yellow (#f59e0b)

// Bordures & Ombres
--border-color, --shadow-sm, --shadow-md, --shadow-lg, --shadow-glow

// Transitions
--transition-fast (0.2s), --transition-smooth (0.3s cubic-bezier)
```

## Polices
- `Crimson Text` (serif) → titres, brand, texte éditorial
- `Inter` (sans-serif)   → UI, labels, boutons, tables, métriques

---

## Thème Keycloak
Custom theme `financeapp` (FreeMarker `.ftl`) dans `keycloak/themes/financeapp/login/`.
Reprend les mêmes variables CSS que l'app Angular.