import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
      return this.http.get<CompanyProfileResponse>(
         `${environment.apiUrl}/company-profile/${ticker.toUpperCase()}`,
      );
   }
}
