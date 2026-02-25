import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private keycloakInstance: Keycloak | undefined;

  async init(): Promise<void> {
    this.keycloakInstance = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId
    });

    try {
      const authenticated = await this.keycloakInstance.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        pkceMethod: 'S256'
      });
      console.log(`[Keycloak] Statut : ${authenticated ? 'Authentifié' : 'Public'}`);
    } catch (error) {
      console.error('[Keycloak] Erreur critique au démarrage', error);
    }
  }

  get instance(): Keycloak {
    if (!this.keycloakInstance) {
      throw new Error('Keycloak n\'est pas initialisé.');
    }
    return this.keycloakInstance;
  }
}