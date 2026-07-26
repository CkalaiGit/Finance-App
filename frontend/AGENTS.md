# FinanceApp — Agent Context

## Projet
Application d'analyse financière Angular avec authentification Keycloak.
Permet aux utilisateurs (PREMIUM / FREEMIUM) de consulter des métriques financières
de sociétés cotées (croissance, valorisation, qualité).

## Stack Technique
| Couche | Technologie |
|--------|-------------|
| Frontend | Angular 20 (standalone components) |
| Auth | Keycloak 26 (PKCE S256, check-sso silencieux) |
| CSS | SCSS + variables CSS custom (design system interne) |
| Tests | Vitest + happy-dom |
| Backend | Spring Boot sur `http://localhost:8081` |
| IdP | Keycloak sur `http://localhost:8080`, realm `dev-realm` |

## Architecture Auth (CRITIQUE — ne jamais contourner)
```
KeycloakService       → Wrapper bas niveau, contient l'instance Keycloak-js
AuthService           → Logique métier (isLoggedIn, getUserProfile, login, logout)
AuthStoreService      → State management RxJS (BehaviorSubject<UserProfile|null>)
authInterceptor       → Ajoute automatiquement Bearer token sur chaque requête HTTP
authGuard             → Protège les routes, redirige vers Keycloak si non connecté
```

## Rôles Utilisateurs
- `PREMIUM` — accès complet aux analyses
- `FREEMIUM` — accès limité aux données publiques

## Utilisateurs de Dev (realm-dev.json)
| Username | Password | Rôle |
|----------|----------|------|
| warren | password | PREMIUM |
| michael | password | FREEMIUM |

## Routes
```
/home           → LandingPageComponent (publique)
/details/:ticker → CompanyDetailsComponent (publique)
/watchlist      → à implémenter (protégée, PREMIUM uniquement)
/about          → à implémenter
```

## API Backend
- `GET /api/v1/companies/:ticker`            → Company
- `GET /api/v1/company-profile/:ticker`       → CompanyProfileResponse
- `GET /api/v1/analysis/:ticker`              → FullMetrics[]
- `GET /api/users/me`                         → UserContext (protégé)

## Modèles TypeScript Clés
- `UserProfile`           → `{ username, email, roles[] }`
- `FullMetrics`           → `{ growth, value, quality, fiscalYearEndDate, marketDataAsOf }`
- `CompanyProfileResponse` → `{ symbol, companyName, industry, image, beta, marketCap, description, price }`

## Design System — Variables CSS (TOUJOURS utiliser ces variables)
```scss
// Backgrounds
--bg-primary, --bg-secondary, --bg-card, --bg-card-hover

// Texte
--text-primary, --text-secondary, --text-muted

// Accents
--accent-blue (#3b82f6), --accent-purple (#8b5cf6),
--accent-cyan, --accent-green, --accent-red, --accent-yellow

// Bordures & ombres
--border-color, --shadow-sm, --shadow-md, --shadow-lg, --shadow-glow

// Transitions
--transition-fast (0.2s), --transition-smooth (0.3s cubic-bezier)
```

## Polices
- `Crimson Text` (serif) → titres, brand, texte éditorial
- `Inter` (sans-serif)   → UI, labels, boutons, code

## Thème Keycloak
Custom theme `financeapp` (FreeMarker `.ftl`) dans `keycloak/themes/financeapp/login/`.
Reprend les mêmes variables CSS que l'app Angular.