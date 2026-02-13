import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { UserProfile } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  /**
   * Récupère l'instance Keycloak injectée globalement dans main.ts
   * On utilise un getter pour s'assurer d'avoir l'instance à jour.
   */
  private get kc(): Keycloak {
    const keycloakInstance = (window as any).keycloak as Keycloak;
    if (!keycloakInstance) {
      throw new Error('Keycloak n\'est pas initialisé. Vérifiez main.ts');
    }
    return keycloakInstance;
  }

  /**
   * Vérifie si l'utilisateur est authentifié
   */
  get isLoggedIn(): boolean {
    return !!this.kc.authenticated;
  }

  /**
   * Récupère le nom d'utilisateur depuis le token
   */
  get username(): string | undefined {
    return this.kc.tokenParsed?.['preferred_username'];
  }

  /**
   * Transforme les données Keycloak en notre interface métier UserProfile
   * C'est cette méthode que ton AuthStore appellera.
   */
  getUserProfile(): UserProfile | null {
    if (!this.isLoggedIn) {
      return null;
    }

    return {
      username: this.username || 'Investisseur Inconnu',
      email: this.kc.tokenParsed?.['email'] || '',
      // Extraction des rôles du Realm (Michael vs Warren)
      roles: this.kc.realmAccess?.roles || []
    };
  }

  /**
   * Redirige vers la page de login Keycloak
   */
  login(): void {
    this.kc.login();
  }

  /**
   * Déconnexion et redirection vers l'accueil
   */
  logout(): void {
    this.kc.logout({ 
      redirectUri: window.location.origin 
    });
  }

  /**
   * Récupère le token JWT pour l'intercepteur HTTP
   */
  getToken(): string | undefined {
    return this.kc.token;
  }

  /**
   * Vérifie si l'utilisateur possède un rôle spécifique
   */
  hasRole(role: string): boolean {
    return this.kc.hasRealmRole(role);
  }
}