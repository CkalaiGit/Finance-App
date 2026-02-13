import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStoreService } from './auth-store.service';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const store = inject(AuthStoreService);
  const router = inject(Router);

  return store.isLoggedIn$.pipe(
    take(1), // On prend la valeur actuelle et on complète
    map(isLoggedIn => {
      if (isLoggedIn) {
        return true;
      }
      
      // Optionnel : rediriger vers l'accueil si non connecté
      // return router.parseUrl('/'); 
      
      // Pour ton projet, Keycloak gère souvent la redirection lui-même
      // via authService.login() si le Guard retourne false.
      return false;
    })
  );
};