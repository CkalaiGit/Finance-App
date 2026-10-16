# FinanceApp — Frontend Angular 20

Dashboard financier moderne construit avec **Angular 20** (standalone components, signals-ready, `ChangeDetectionStrategy.OnPush`), authentification **Keycloak 26 (PKCE S256)**, et consommation d'API REST **Spring Boot 4**.

---

## 🚀 Stack Technique

- **Framework** : Angular 20 (standalone components, OnPush detection)
- **Authentification** : Keycloak 26 via `keycloak-js` (OIDC / OAuth2 + PKCE S256 & Silent Check-SSO)
- **State & Flow** : RxJS (`BehaviorSubject`, `forkJoin`, `switchMap`)
- **Style** : SCSS avec variables CSS custom (design system sombre fintech, glassmorphism)
- **Tests** : Vitest + happy-dom (`npm test`)

---

## 🛠️ Installation & Démarrage

### Prérequis
- Node.js 20+
- Backend Spring Boot lancé sur `http://localhost:8081`
- Instance Keycloak lancée sur `http://localhost:8080` (Realm `dev-realm`)

### Dépendances
```bash
npm install
```

### Lancement en mode dev
```bash
npm start
```
L'application est disponible sur `http://localhost:4200`.

---

## 🧪 Tests Unitaires

Pour lancer les tests avec Vitest :
```bash
npm test
```

Pour lancer les tests en mode watch :
```bash
npm run test:watch
```

---

## 📱 Pages & Fonctionnalités

1. **Accueil (`/home`)** : Landing page publique présentant l'application et la barre de recherche d'entreprises.
2. **Détails Entreprise (`/details/:ticker`)** : Analyse financière détaillée (Croissance, Valorisation, Qualité) avec bouton toggle dynamique *"Ajouter / Retirer de la watchlist"*.
3. **Watchlist (`/watchlist`)** : Page protégée par `authGuard` affichant la liste de suivi de l'utilisateur sous forme de **tableau tabulé à 3 onglets** (Croissance, Valorisation, Qualité) avec appels de métriques parallélisés via `forkJoin`.

---

## 🔐 Architecture Authentification

- `KeycloakService` : Gère le cycle de vie du SDK `keycloak-js`.
- `AuthService` : Logique d'authentification métier (`login`, `logout`, `getUserProfile`).
- `AuthStoreService` : State management réactif de l'état de connexion (`user$`, `isLoggedIn$`).
- `authInterceptor` : Intercepteur HTTP automatique injectant le header `Authorization: Bearer <token>`.
- `authGuard` : Functional route guard redirigeant vers Keycloak si l'utilisateur n'est pas authentifié.

---

## 📄 Documentation Complète

Pour un guide détaillé sur l'architecture et les conventions de code, consultez [AGENTS.md](./AGENTS.md).