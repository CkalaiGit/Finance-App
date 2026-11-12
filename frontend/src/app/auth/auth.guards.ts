import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthStoreService } from './auth-store.service';
import { map, take } from 'rxjs';

/**
 * Protège une route sans jamais ouvrir Keycloak : un visiteur non connecté est
 * ramené sur /home, avec l'URL demandée dans `returnUrl`. La page d'accueil
 * affiche alors une invitation à se connecter.
 */
export const authGuard: CanActivateFn = (_route, state) => {
   const store = inject(AuthStoreService);
   const router = inject(Router);

   return store.isLoggedIn$.pipe(
      take(1),
      map((isLoggedIn): boolean | UrlTree => {
         if (isLoggedIn) {
            return true;
         }
         return router.createUrlTree(['/home'], {
            queryParams: { returnUrl: state.url },
         });
      }),
   );
};

export const roleGuard = (role: string): CanActivateFn => {
   return (): boolean | UrlTree => {
      const store = inject(AuthStoreService);
      const router = inject(Router);

      if (store.hasRole(role)) {
         return true;
      }

      return router.createUrlTree(['/home']);
   };
};
