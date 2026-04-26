import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Company, CompanyDisplayDTO } from '../models/company.model';

/**
 * Service de gestion des entreprises
 * 
 * @description
 * Encapsule toute la logique métier liée aux entreprises :
 * - Récupération des données (actuellement mock, prêt pour API)
 * - Transformation en DTOs pour l'affichage
 * - Logique de mapping couleur par industrie
 * 
 * @example
 * ```typescript
 * constructor(private companyService: CompanyService) {}
 * 
 * ngOnInit() {
 *   this.companyService.getFeaturedCompanies()
 *     .subscribe(companies => this.companies = companies);
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class CompanyService {

  /**
   * Mapping industrie → couleur de badge
   * 
   * @description
   * Règle métier : chaque secteur a une couleur associée pour
   * faciliter l'identification visuelle.
   */
  private readonly industryColorMap: Record<string, CompanyDisplayDTO['colorVariant']> = {
    'Semi-conducteurs': 'green',
    'Services Internet': 'blue',
    'Logiciels & Cloud': 'purple',
    'E-commerce': 'cyan',
    'Finance': 'blue',
    'Santé': 'red',
    'Énergie': 'green',
    // Extensible : ajoutez vos secteurs ici
  };

  private readonly baseUrl = 'http://localhost:8081/api/v1/companies';

  constructor(private readonly http: HttpClient) { }

  /**
   * Récupère les entreprises vedettes pour la landing page
   * 
   * @returns Observable de CompanyDisplayDTO[] avec métadonnées UI
   * 
   * @description
   * Appelle l'API pour obtenir les entreprises vedettes.
   * Transforme les Company en CompanyDisplayDTO avec colorVariant.
   * 
   * @example
   * ```typescript
   * this.companyService.getFeaturedCompanies()
   *   .subscribe({
   *     next: (companies) => this.companies = companies,
   *     error: (err) => console.error(err)
   *   });
   * ```
   */
  getFeaturedCompanies(): Observable<CompanyDisplayDTO[]> {
    return this.http.get<Company[]>(`${this.baseUrl}/featured`).pipe(
      map(companies => companies.map(c => this.toDisplayDTO(c))),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recherche des entreprises par terme
   * 
   * @param searchTerm - Terme de recherche (ticker ou nom)
   * @returns Observable de CompanyDisplayDTO[]
   * 
   * @description
   * Appelle l'API avec query params.
   * 
   * @example
   * ```typescript
   * this.companyService.searchCompanies('NVDA')
   *   .subscribe(results => console.log(results));
   * ```
   */
  searchCompanies(searchTerm: string): Observable<CompanyDisplayDTO[]> {
    if (!searchTerm.trim()) {
      return of([]);
    }

    const term = encodeURIComponent(searchTerm.trim());

    return this.http.get<Company[]>(`${this.baseUrl}/search?q=${term}`).pipe(
      map(companies => companies.map(c => this.toDisplayDTO(c))),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Récupère une entreprise par son ticker
   * 
   * @param ticker - Symbole boursier (ex: "AAPL")
   * @returns Observable de CompanyDisplayDTO ou null si non trouvée
   * 
   * @description
   * Fait un appel API vers GET /api/companies/:ticker
   * 
   * @example
   * ```typescript
   * this.companyService.getByTicker('MSFT')
   *   .subscribe(company => {
   *     if (company) {
   *       console.log(company.name);
   *     }
   *   });
   * ```
   */
  getByTicker(ticker: string): Observable<CompanyDisplayDTO | null> {
    return this.http.get<Company>(`${this.baseUrl}/${ticker.toUpperCase()}`).pipe(
      map(company => company ? this.toDisplayDTO(company) : null),
      catchError(this.handleError.bind(this))
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Erreur API:', error);
    return throwError(() => new Error('Erreur lors du chargement des données'));
  }

  /**
   * Transforme un Company en CompanyDisplayDTO
   * 
   * @param company - Données métier de l'entreprise
   * @returns DTO avec métadonnées UI (colorVariant)
   * 
   * @private
   * @description
   * Applique la logique de mapping industrie → couleur.
   * Couleur par défaut : 'blue' si industrie inconnue.
   */
  private toDisplayDTO(company: Company): CompanyDisplayDTO {
    return {
      ...company,
      colorVariant: this.getColorForIndustry(company.industry)
    };
  }

  /**
   * Détermine la couleur du badge selon l'industrie
   * 
   * @param industry - Nom de l'industrie
   * @returns Variante de couleur pour le badge
   * 
   * @private
   * @description
   * Logique métier centralisée pour la cohérence visuelle.
   * Extensible via industryColorMap.
   */
  private getColorForIndustry(industry: string): CompanyDisplayDTO['colorVariant'] {
    return this.industryColorMap[industry] || 'blue';
  }


}