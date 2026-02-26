import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, delay } from 'rxjs/operators';
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

  /**
   * Données mockées des entreprises vedettes
   * 
   * @description
   * En production, ces données viendraient d'une API via HttpClient.
   * Structure prête pour migration vers un appel HTTP.
   * 
   * ⚠️ TODO : Remplacer par un appel API réel plus tard
   */
  private readonly mockCompanies: Company[] = [
    {
      ticker: 'NVDA',
      name: 'NVIDIA Corporation',
      industry: 'Semi-conducteurs',
      marketCap: 1200000000000, // 1.2T $
      currentPrice: 875.50
    },
    {
      ticker: 'GOOGL',
      name: 'Alphabet Inc.',
      industry: 'Services Internet',
      marketCap: 1800000000000, // 1.8T $
      currentPrice: 142.30
    },
    {
      ticker: 'MSFT',
      name: 'Microsoft Corporation',
      industry: 'Logiciels & Cloud',
      marketCap: 2900000000000, // 2.9T $
      currentPrice: 398.75
    }
  ];

  constructor() {
    console.log('✅ CompanyService initialisé avec', this.mockCompanies.length, 'entreprises');
  }

  /**
   * Récupère les entreprises vedettes pour la landing page
   * 
   * @returns Observable de CompanyDisplayDTO[] avec métadonnées UI
   * 
   * @description
   * Simule un appel API avec delay pour réalisme.
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
    return of(this.mockCompanies).pipe(
      delay(300), // Simule latence réseau (enlevez en prod)
      map(companies => companies.map(c => this.toDisplayDTO(c)))
    );
  }

  /**
   * Recherche des entreprises par terme
   * 
   * @param searchTerm - Terme de recherche (ticker ou nom)
   * @returns Observable de CompanyDisplayDTO[]
   * 
   * @description
   * Recherche insensible à la casse sur le ticker et le nom.
   * Prêt pour être remplacé par un appel API avec query params.
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

    const term = searchTerm.toLowerCase().trim();
    
    return of(this.mockCompanies).pipe(
      delay(200),
      map(companies => 
        companies
          .filter(c => 
            c.ticker.toLowerCase().includes(term) ||
            c.name.toLowerCase().includes(term)
          )
          .map(c => this.toDisplayDTO(c))
      )
    );
  }

  /**
   * Récupère une entreprise par son ticker
   * 
   * @param ticker - Symbole boursier (ex: "AAPL")
   * @returns Observable de CompanyDisplayDTO ou null si non trouvée
   * 
   * @description
   * Prêt pour être remplacé par un GET /api/companies/:ticker
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
    const company = this.mockCompanies.find(
      c => c.ticker.toLowerCase() === ticker.toLowerCase()
    );
    
    return of(company ? this.toDisplayDTO(company) : null).pipe(
      delay(150)
    );
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

  // ========================================
  // MÉTHODES FUTURES (prêtes pour l'API)
  // ========================================

  /**
   * @future Migration vers HttpClient
   * 
   * Une fois le backend prêt, remplacez les méthodes ci-dessus par :
   * 
   * ```typescript
   * constructor(private http: HttpClient) {}
   * 
   * getFeaturedCompanies(): Observable<CompanyDisplayDTO[]> {
   *   return this.http.get<Company[]>('/api/companies/featured')
   *     .pipe(
   *       map(companies => companies.map(c => this.toDisplayDTO(c))),
   *       catchError(this.handleError)
   *     );
   * }
   * 
   * private handleError(error: HttpErrorResponse): Observable<never> {
   *   console.error('Erreur API:', error);
   *   return throwError(() => new Error('Erreur lors du chargement des données'));
   * }
   * ```
   */
}