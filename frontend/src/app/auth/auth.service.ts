import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // On récupère l'instance initialisée dans main.ts
  private get kc(): Keycloak {
    return (window as any).keycloak;
  }

  get isLoggedIn(): boolean {
    return !!this.kc?.authenticated;
  }

  get username(): string | undefined {
    return this.kc?.tokenParsed?.['preferred_username'];
  }

  login(): void {
    this.kc.login();
  }

  logout(): void {
    // Redirige vers l'accueil après la déconnexion
    this.kc.logout({ redirectUri: window.location.origin });
  }

  getToken(): string | undefined {
    return this.kc?.token;
  }
}