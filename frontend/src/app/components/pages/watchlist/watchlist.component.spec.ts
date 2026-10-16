import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { WatchlistComponent } from './watchlist.component';
import { WatchlistService } from '../../../services/watchlist.service';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { AuthStoreService } from '../../../auth/auth-store.service';
import { WatchlistResponse } from '../../../models/watchlist.model';
import { FullMetrics } from '../../../models/analysis.model';
import { UserProfile } from '../../../models/user.model';

describe('WatchlistComponent', () => {
   let component: WatchlistComponent;
   let fixture: ComponentFixture<WatchlistComponent>;
   let mockWatchlistService: {
      getWatchlist: ReturnType<typeof vi.fn>;
      addTicker: ReturnType<typeof vi.fn>;
      removeTicker: ReturnType<typeof vi.fn>;
   };
   let mockAnalysisService: {
      getMetrics: ReturnType<typeof vi.fn>;
   };
   let mockAuthStoreService: {
      user$: Observable<UserProfile | null>;
   };

   const sampleWatchlistResponse: WatchlistResponse = {
      keycloakId: 'user-123',
      tickers: ['AAPL', 'MSFT'],
   };

   const sampleMetrics: FullMetrics = {
      growth: { revenueGrowth3Y: 0.15, ebitdaGrowth3Y: 0.12, epsGrowth3Y: 0.1, fcfGrowth: 0.08 },
      value: { evToEbit: 22.5, peRatioTTM: 28.3, pegRatioForward: 1.5, evToSales: 6.2 },
      quality: { roic: 0.25, operatingMargin: 0.3, netDebtToEbitda: 0.8, freeCashFlowMargin: 0.22, sgaToRevenue: 0.1 },
      fiscalYearEndDate: '2025-12-31',
      marketDataAsOf: '2026-01-01',
   };

   beforeEach(async () => {
      mockWatchlistService = {
         getWatchlist: vi.fn().mockReturnValue(of(sampleWatchlistResponse)),
         addTicker: vi.fn().mockReturnValue(
            of({
               keycloakId: 'user-123',
               tickers: ['AAPL', 'MSFT', 'NVDA'],
            }),
         ),
         removeTicker: vi.fn().mockReturnValue(
            of({
               keycloakId: 'user-123',
               tickers: ['MSFT'],
            }),
         ),
      };

      mockAnalysisService = {
         getMetrics: vi.fn().mockReturnValue(of(sampleMetrics)),
      };

      mockAuthStoreService = {
         user$: of({ username: 'Warren', email: 'warren@dev.com', roles: ['PREMIUM'] }),
      };

      await TestBed.configureTestingModule({
         imports: [WatchlistComponent],
         providers: [
            provideRouter([]),
            { provide: WatchlistService, useValue: mockWatchlistService },
            { provide: FinancialAnalysisService, useValue: mockAnalysisService },
            { provide: AuthStoreService, useValue: mockAuthStoreService },
         ],
      }).compileComponents();

      fixture = TestBed.createComponent(WatchlistComponent);
      component = fixture.componentInstance;
   });

   it('should create the component', () => {
      expect(component).toBeTruthy();
   });

   it('should load watchlist and metrics on init', () => {
      fixture.detectChanges(); // triggers ngOnInit

      expect(mockWatchlistService.getWatchlist).toHaveBeenCalled();
      expect(mockAnalysisService.getMetrics).toHaveBeenCalledWith('AAPL');
      expect(mockAnalysisService.getMetrics).toHaveBeenCalledWith('MSFT');
      expect(component.items.length).toBe(2);
      expect(component.items[0].ticker).toBe('AAPL');
      expect(component.items[0].metrics).toEqual(sampleMetrics);
      expect(component.isLoading).toBe(false);
   });

   it('should handle error when loading watchlist fails', () => {
      mockWatchlistService.getWatchlist.mockReturnValue(
         throwError(() => new Error('Server error')),
      );

      fixture.detectChanges();

      expect(component.isLoading).toBe(false);
      expect(component.errorMessage).toBe('Impossible de charger votre watchlist.');
   });

   it('should convert input to uppercase', () => {
      component.onTickerInput('googl');
      expect(component.newTicker).toBe('GOOGL');
   });

   it('should add a ticker successfully', () => {
      fixture.detectChanges();
      component.newTicker = 'NVDA';

      component.addTicker();

      expect(mockWatchlistService.addTicker).toHaveBeenCalledWith('NVDA');
      expect(component.newTicker).toBe('');
   });

   it('should prevent adding duplicate ticker', () => {
      fixture.detectChanges();
      component.newTicker = 'AAPL';

      component.addTicker();

      expect(mockWatchlistService.addTicker).not.toHaveBeenCalled();
      expect(component.errorMessage).toContain('déjà présent');
   });

   it('should remove a ticker successfully', () => {
      fixture.detectChanges();
      const mockEvent = { stopPropagation: vi.fn() } as unknown as Event;

      component.removeTicker(mockEvent, 'AAPL');

      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(mockWatchlistService.removeTicker).toHaveBeenCalledWith('AAPL');
      expect(component.items.map((i) => i.ticker)).toEqual(['MSFT']);
   });

   it('should format percent and ratio correctly', () => {
      expect(component.formatPercent(0.1567)).toBe('15.67%');
      expect(component.formatPercent(null)).toBe('—');
      expect(component.formatRatio(24.5678)).toBe('24.57x');
      expect(component.formatRatio(undefined)).toBe('—');
   });
});
