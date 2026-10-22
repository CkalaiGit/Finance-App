# Rules FinanceApp — Toujours actives

## Angular — Conventions absolues

- TOUJOURS créer des **standalone components** (`standalone: true`)
- TOUJOURS utiliser `ChangeDetectionStrategy.OnPush`
- TOUJOURS déclarer les dépendances injectées en `private readonly`
- TOUJOURS utiliser `inject()` ou le constructeur, jamais `(window as any)`
- Préférer le pipe `async` dans les templates plutôt que `.subscribe()` dans les composants
- Utiliser `@if`, `@for`, `@switch` (syntaxe Angular 17+ deferrable views) — jamais `*ngIf`, `*ngFor`
- Pour les Observables exposés au template : nommer avec le suffixe `$` (ex: `user$`, `metrics$`)

## TypeScript

- `readonly` sur toutes les propriétés qui ne changent pas après l'init
- Pas de `any` — typer explicitement ou utiliser `unknown`
- Interfaces dans `src/app/models/` — un fichier par domaine métier
- Ne jamais muter un objet d'état directement — passer par le `BehaviorSubject`

## Auth — Règles critiques

- Ne JAMAIS accéder à `(window as any).keycloak` — utiliser `KeycloakService.instance`
- Ne JAMAIS exposer le token JWT dans les logs ou l'UI
- Pour protéger une route → utiliser `canActivate: [authGuard]`
- Pour vérifier un rôle dans un template → utiliser `authStore.hasRole('PREMIUM')`
- `AuthStoreService` est le seul point de vérité pour l'état utilisateur dans l'UI

## SCSS & Design

- JAMAIS de couleurs en dur (`#3b82f6`) — utiliser les variables CSS (`var(--accent-blue)`)
- Les styles globaux vont dans `src/styles.scss`
- Les styles spécifiques à un composant vont dans le `.scss` du composant
- Les gradients de base : `linear-gradient(135deg, var(--accent-blue) 0%, var(--accent-purple) 100%)`
- Breakpoints : mobile < 768px, tablet < 1024px, desktop ≥ 1024px

## Accessibilité (RGAA)

- Chaque élément interactif doit avoir un `aria-label` explicite
- Images décoratives → `aria-hidden="true"`
- Utiliser `.sr-only` pour le texte visible uniquement aux lecteurs d'écran
- Focus visible obligatoire (déjà géré par `*:focus-visible` dans styles.scss)
- Contraste minimum 4.5:1 pour le texte normal, 3:1 pour les grands titres

## Tests (Vitest)

- Chaque service → au moins un fichier `.spec.ts`
- Utiliser `vi.fn()` pour les mocks (jamais `jasmine.createSpy`)
- Importer `{ describe, beforeEach, it, expect, vi }` depuis `vitest`
- Pour les tests de composants Angular : utiliser `TestBed` + `ComponentFixture`
- Mock de `KeycloakService` → toujours fournir `{ instance: mockKeycloakInstance }`

## Structure des fichiers

```
src/app/
├── auth/           → Services d'authentification uniquement
├── components/
│   ├── layout/     → Header, Footer, Shell
│   └── pages/      → Un dossier par page (html + scss + ts + spec.ts)
├── models/         → Interfaces TypeScript (pas de classes)
└── services/       → Services métier (API calls)
```

## API HTTP

- Base URL backend : `http://localhost:8081`
- Le token est injecté automatiquement par `authInterceptor` — ne pas l'ajouter manuellement
- Utiliser `catchError(() => of(null))` pour les erreurs non critiques dans les composants
- Préférer `switchMap` pour les appels HTTP déclenchés par des paramètres de route