import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { UserProfile } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthStoreService {
  // Source de vérité privée (Point 2.1 : État encapsulé)
  private readonly userSubject$ = new BehaviorSubject<UserProfile | null>(null);

  // Exposition publique en lecture seule (Point 5 : Immuabilité)
  readonly user$: Observable<UserProfile | null> = this.userSubject$.asObservable();

  // Sélecteur pour l'état de connexion (Point 3 : Performance via RxJS)
  readonly isLoggedIn$: Observable<boolean> = this.user$.pipe(
    map(user => !!user)
  );

  constructor(private readonly authService: AuthService) {
    // Initialisation immédiate de l'état au chargement de l'application
    this.refreshState();
  }

  /**
   * Met à jour le BehaviorSubject avec les données actuelles de Keycloak
   */
  refreshState(): void {
    const profile = this.authService.getUserProfile();
    this.userSubject$.next(profile);
  }

  /**
   * Délègue l'action de login et rafraîchit l'état
   */
  login(): void {
    this.authService.login();
  }

  /**
   * Délègue le logout
   */
  logout(): void {
    this.authService.logout();
  }

  /**
   * Helper pour vérifier un rôle sans logique complexe dans le composant
   */
  hasRole(role: string): boolean {
    const currentUser = this.userSubject$.getValue();
    return currentUser?.roles.includes(role) ?? false;
  }
}