import { Injectable, inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { UserProfile } from '../models/user.model';
import { KeycloakService } from './keycloak.service'; // Importe ton nouveau service

@Injectable({
   providedIn: 'root',
})
export class AuthService {
   // 1. On injecte le service technique qui contient l'instance réelle
   private readonly kcService = inject(KeycloakService);

   /**
    * Récupère l'instance Keycloak depuis le service technique.
    * On n'utilise plus (window as any).
    */
   private get kc(): Keycloak {
      return this.kcService.instance;
   }

   get isLoggedIn(): boolean {
      return !!this.kc.authenticated;
   }

   get username(): string | undefined {
      return this.kc.tokenParsed?.['preferred_username'];
   }

   getUserProfile(): UserProfile | null {
      if (!this.isLoggedIn) {
         return null;
      }

      return {
         username: this.username || 'Investisseur Inconnu',
         email: this.kc.tokenParsed?.['email'] || '',
         roles: this.kc.realmAccess?.roles || [],
      };
   }

   /**
    * Ouvre la page de connexion Keycloak.
    * @param redirectUri URL absolue de retour après connexion. Sans paramètre,
    *                    Keycloak revient sur la page courante.
    */
   login(redirectUri?: string): void {
      if (redirectUri) {
         this.kc.login({ redirectUri });
         return;
      }
      this.kc.login();
   }

   logout(): void {
      this.kc.logout({
         redirectUri: window.location.origin + '/home',
      });
   }

   getToken(): string | undefined {
      return this.kc.token;
   }

   hasRole(role: string): boolean {
      return this.kc.hasRealmRole(role);
   }
}
