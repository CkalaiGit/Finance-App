import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthStoreService } from './auth-store.service';
import { AuthService } from './auth.service';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
   const store = inject(AuthStoreService);
   const authService = inject(AuthService);

   return store.isLoggedIn$.pipe(
      take(1),
      map((isLoggedIn) => {
         if (isLoggedIn) {
            return true;
         }
         authService.login();
         return false;
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
