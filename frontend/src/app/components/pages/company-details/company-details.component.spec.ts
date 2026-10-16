import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { CompanyDetailsComponent } from './company-details.component';
import { CompanyService } from '../../../services/company.service';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { WatchlistService } from '../../../services/watchlist.service';
import { CompanyProfileResponse } from '../../../models/company.model';
import { FullMetrics } from '../../../models/analysis.model';
import { ApiError } from '../../../models/api-error.model';

describe('CompanyDetailsComponent', () => {
   let component: CompanyDetailsComponent;
   let fixture: ComponentFixture<CompanyDetailsComponent>;
   let paramMapSubject: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

   let companyServiceMock: {
      getCompanyProfile: ReturnType<typeof vi.fn>;
   };
   let analysisServiceMock: {
      getMetrics: ReturnType<typeof vi.fn>;
   };
   let watchlistServiceMock: {
      checkExists: ReturnType<typeof vi.fn>;
      addTicker: ReturnType<typeof vi.fn>;
      removeTicker: ReturnType<typeof vi.fn>;
   };

   const sampleProfile: CompanyProfileResponse = {
      symbol: 'AAPL',
      companyName: 'Apple Inc.',
      industry: 'Technology',
      image: 'https://example.com/aapl.png',
      beta: 1.2,
      marketCap: 3000000000000,
      description: 'Apple designs consumer electronics.',
      price: 195.5,
   };

   const sampleMetrics: FullMetrics = {
      growth: { revenueGrowth3Y: 0.15, ebitdaGrowth3Y: 0.12, epsGrowth3Y: 0.1, fcfGrowth: 0.08 },
      value: { evToEbit: 22.5, peRatioTTM: 28.3, pegRatioForward: 1.5, evToSales: 6.2 },
      quality: { roic: 0.25, operatingMargin: 0.3, netDebtToEbitda: 0.8, freeCashFlowMargin: 0.22, sgaToRevenue: 0.1 },
      fiscalYearEndDate: '2025-12-31',
      marketDataAsOf: '2026-01-01',
   };

   beforeEach(async () => {
      paramMapSubject = new BehaviorSubject(convertToParamMap({ ticker: 'AAPL' }));

      companyServiceMock = {
         getCompanyProfile: vi.fn().mockReturnValue(of(sampleProfile)),
      };
      analysisServiceMock = {
         getMetrics: vi.fn().mockReturnValue(of(sampleMetrics)),
      };
      watchlistServiceMock = {
         checkExists: vi.fn().mockReturnValue(of({ ticker: 'AAPL', exists: false })),
         addTicker: vi.fn().mockReturnValue(of({ keycloakId: 'u1', tickers: ['AAPL'] })),
         removeTicker: vi.fn().mockReturnValue(of({ keycloakId: 'u1', tickers: [] })),
      };

      await TestBed.configureTestingModule({
         imports: [CompanyDetailsComponent],
         providers: [
            provideRouter([]),
            { provide: ActivatedRoute, useValue: { paramMap: paramMapSubject.asObservable() } },
            { provide: CompanyService, useValue: companyServiceMock },
            { provide: FinancialAnalysisService, useValue: analysisServiceMock },
            { provide: WatchlistService, useValue: watchlistServiceMock },
         ],
      }).compileComponents();
   });

   it('should display loading state while fetching company data', () => {
      companyServiceMock.getCompanyProfile.mockReturnValue(new BehaviorSubject(null).pipe()); // or NEVER
      fixture = TestBed.createComponent(CompanyDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const loadingEl = compiled.querySelector('[data-testid="loading-state"]');
      expect(loadingEl).toBeTruthy();
      expect(compiled.textContent).toContain('Chargement des données...');
   });

   it('should display data state when company profile and metrics are successfully loaded', async () => {
      fixture = TestBed.createComponent(CompanyDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const dataStateEl = compiled.querySelector('[data-testid="data-state"]');
      expect(dataStateEl).toBeTruthy();
      expect(compiled.querySelector('.company-name-large')?.textContent).toContain('Apple Inc.');
      expect(compiled.querySelector('.company-ticker-large')?.textContent).toContain('AAPL');
   });

   it('should display error state when company service fails', async () => {
      const error: ApiError = {
         status: 404,
         title: 'Non trouvé',
         detail: 'Société introuvable',
      };
      companyServiceMock.getCompanyProfile.mockReturnValue(throwError(() => error));

      fixture = TestBed.createComponent(CompanyDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorStateEl = compiled.querySelector('[data-testid="error-state"]');
      expect(errorStateEl).toBeTruthy();
      expect(compiled.textContent).toContain('Non trouvé');
      expect(compiled.textContent).toContain('Société introuvable');
   });

   it('should display empty state when ticker is missing', async () => {
      paramMapSubject.next(convertToParamMap({ ticker: '' }));

      fixture = TestBed.createComponent(CompanyDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const emptyStateEl = compiled.querySelector('[data-testid="empty-state"]');
      expect(emptyStateEl).toBeTruthy();
      expect(compiled.textContent).toContain('Aucune entreprise trouvée');
   });
});
