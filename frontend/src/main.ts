import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import Keycloak from 'keycloak-js';

// 1. Configuration liée à ton infra-keycloak (Docker)
const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'dev-realm',
  clientId: 'frontend-client'
});

// 2. Lancement du processus d'authentification
keycloak.init({
  onLoad: 'check-sso', // Vérifie si l'utilisateur est déjà loggé au chargement
  silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
  pkceMethod: 'S256' // Utilisation du PKCE (Expert standard)
}).then((authenticated) => {
  console.log(`[Keycloak] Statut : ${authenticated ? 'Authentifié' : 'Public'}`);
  
  // On expose l'instance pour notre service Angular
  (window as any).keycloak = keycloak;

  // 3. On démarre Angular seulement ici
  bootstrapApplication(AppComponent, appConfig)
    .catch((err) => console.error(err));
}).catch(err => {
  console.error('[Keycloak] Erreur critique au démarrage', err);
});