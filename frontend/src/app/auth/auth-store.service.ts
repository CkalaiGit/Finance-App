import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { UserProfile } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthStoreService {
  private readonly userSubject$ = new BehaviorSubject<UserProfile | null>(null);

  readonly user$: Observable<UserProfile | null> = this.userSubject$.asObservable();

  readonly isLoggedIn$: Observable<boolean> = this.user$.pipe(
    map(user => !!user)
  );

  constructor(private readonly authService: AuthService) {
    this.refreshState();
  }

  refreshState(): void {
    const profile = this.authService.getUserProfile();
    this.userSubject$.next(profile);
  }

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }

  hasRole(role: string): boolean {
    const currentUser = this.userSubject$.getValue();
    return currentUser?.roles.includes(role) ?? false;
  }
}