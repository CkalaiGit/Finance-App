import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CompanyProfileResponse } from '../models/company.model';
import { environment } from '../../environments/environment';

/**
 * Service de gestion des entreprises
 *
 * @description
 * Encapsule la logique métier liée au profil des entreprises.
 */
@Injectable({
   providedIn: 'root',
})
export class CompanyService {
   private readonly http = inject(HttpClient);

   /**
    * Récupère le profil complet d'une entreprise
    *
    * @param ticker - Symbole boursier
    * @returns Observable de CompanyProfileResponse
    */
   getCompanyProfile(ticker: string): Observable<CompanyProfileResponse> {
      return this.http
         .get<CompanyProfileResponse>(
            `${environment.apiUrl}/company-profile/${ticker.toUpperCase()}`,
         )
         .pipe(catchError(this.handleError.bind(this)));
   }

   private handleError(error: HttpErrorResponse): Observable<never> {
      console.error('Erreur API:', error);
      return throwError(
         () => new Error('Erreur lors du chargement des données'),
      );
   }
}
