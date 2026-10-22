import { TestBed } from '@angular/core/testing';
import {
   HttpTestingController,
   provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { FinancialAnalysisService } from './financial-analysis.service';
import { FullMetrics } from '../models/analysis.model';
import { environment } from '../../environments/environment';

describe('FinancialAnalysisService', () => {
   let service: FinancialAnalysisService;
   let httpMock: HttpTestingController;

   const mockMetrics: FullMetrics = {
      growth: {
         revenueGrowth3Y: 0.15,
         ebitdaGrowth3Y: 0.12,
         epsGrowth3Y: 0.1,
         fcfGrowth: 0.08,
      },
      value: {
         evToEbit: 22.5,
         peRatioTTM: 28.3,
         pegRatioForward: 1.5,
         evToSales: 6.2,
      },
      quality: {
         roic: 0.25,
         operatingMargin: 0.3,
         netDebtToEbitda: 0.8,
         freeCashFlowMargin: 0.22,
         sgaToRevenue: 0.1,
      },
      fiscalYearEndDate: '2025-12-31',
      marketDataAsOf: '2026-01-01',
   };

   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            FinancialAnalysisService,
            provideHttpClient(),
            provideHttpClientTesting(),
         ],
      });

      service = TestBed.inject(FinancialAnalysisService);
      httpMock = TestBed.inject(HttpTestingController);
   });

   afterEach(() => {
      httpMock.verify();
   });

   it('should be created', () => {
      expect(service).toBeTruthy();
   });

   it('should return the first metrics element when data is present', () => {
      service.getMetrics('AAPL').subscribe({
         next: (metrics) => {
            expect(metrics).toEqual(mockMetrics);
         },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/analysis/AAPL`);
      expect(req.request.method).toBe('GET');
      req.flush([mockMetrics]);
   });

   it('should return null when metrics array is empty', () => {
      service.getMetrics('AAPL').subscribe({
         next: (metrics) => {
            expect(metrics).toBeNull();
         },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/analysis/AAPL`);
      req.flush([]);
   });

   it('should propagate HTTP error when API call fails', () => {
      service.getMetrics('FAIL').subscribe({
         next: () => expect.unreachable('Should have failed'),
         error: (error) => {
            expect(error.status).toBe(500);
         },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/analysis/FAIL`);
      req.flush({ detail: 'Server Error' }, { status: 500, statusText: 'Server Error' });
   });
});
