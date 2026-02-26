import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStoreService } from './auth-store.service';
import { AuthService } from './auth.service'; // On injecte le service pour le login
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const store = inject(AuthStoreService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return store.isLoggedIn$.pipe(
    take(1), 
    map(isLoggedIn => {
      if (isLoggedIn) {
        return true;
      }
      authService.login(); 
      return false;
    })
  );
};