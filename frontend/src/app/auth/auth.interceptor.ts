import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, from, throwError } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { KeycloakService } from './keycloak.service';
import { environment } from '../../environments/environment';
import { ApiError } from '../models/api-error.model';

let refreshInProgress$: Observable<boolean> | null = null;

function getRefreshToken$(keycloakService: KeycloakService): Observable<boolean> {
   if (!refreshInProgress$) {
      refreshInProgress$ = from(keycloakService.instance.updateToken(30)).pipe(
         shareReplay({ bufferSize: 1, refCount: false }),
         finalize(() => {
            refreshInProgress$ = null;
         }),
      );
   }
   return refreshInProgress$;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
   // N'ajouter Authorization que pour les requêtes vers environment.apiUrl
   if (!req.url.startsWith(environment.apiUrl)) {
      return next(req);
   }

   const authService = inject(AuthService);
   const keycloakService = inject(KeycloakService);

   return getRefreshToken$(keycloakService).pipe(
      catchError((_err: unknown) => {
         authService.login();
         const apiError: ApiError = {
            status: 401,
            title: 'Session expirée',
            detail: 'Impossible de renouveler la session. Veuillez vous reconnecter.',
         };
         return throwError(() => apiError);
      }),
      switchMap(() => {
         const token = authService.getToken();
         const cloned = token
            ? req.clone({
                 setHeaders: {
                    Authorization: `Bearer ${token}`,
                 },
              })
            : req;
         return next(cloned);
      }),
   );
};
