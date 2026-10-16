import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models/api-error.model';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
   return next(req).pipe(
      catchError((error: unknown) => {
         let apiError: ApiError;

         if (error instanceof HttpErrorResponse) {
            const body =
               typeof error.error === 'object' && error.error !== null
                  ? (error.error as Record<string, unknown>)
                  : null;

            apiError = {
               status:
                  typeof body?.['status'] === 'number'
                     ? (body['status'] as number)
                     : error.status,
               title:
                  (typeof body?.['title'] === 'string' && body['title']) ||
                  error.statusText ||
                  'Erreur réseau',
               detail:
                  (typeof body?.['detail'] === 'string' && body['detail']) ||
                  (typeof error.error === 'string' ? error.error : '') ||
                  error.message ||
                  'Une erreur inattendue est survenue.',
            };
         } else if (error instanceof Error) {
            apiError = {
               status: 0,
               title: 'Erreur',
               detail: error.message,
            };
         } else {
            apiError = {
               status: 0,
               title: 'Erreur inconnue',
               detail: 'Une erreur inattendue est survenue.',
            };
         }

         return throwError(() => apiError);
      }),
   );
};
