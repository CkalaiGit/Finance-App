import { TestBed } from '@angular/core/testing';
import {
   HttpTestingController,
   provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { InsightService } from './insight.service';
import { IngestionStatus, Insight } from '../models/insight.model';
import { environment } from '../../environments/environment';

describe('InsightService', () => {
   let service: InsightService;
   let httpMock: HttpTestingController;

   const mockInsight: Insight = {
      ticker: 'AAPL',
      formType: '10-K',
      period: 'FY 2025',
      accessionNumber: '0000320193-25-000106',
      synthesePerformance: 'Croissance solide tirée par les Services.',
      analyseMargesEtDette: 'Marge brute record et dette sous contrôle.',
      risquesPrincipaux: ['Tensions géopolitiques', 'Concurrence IA'],
      guidanceManagement: 'Croissance continue attendue pour 2026.',
      chaineApprovisionnement: 'Diversification en Inde et au Vietnam.',
      faitsMarquants: ['Nouveaux records de ventes d’iPhone'],
      generatedAt: '2026-02-01T10:00:00Z',
   };

   const mockStatus: IngestionStatus = {
      ticker: 'AAPL',
      ready: true,
      inProgress: false,
      count: 1,
      lastGenerated: '2026-02-01T10:00:00Z',
   };

   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            InsightService,
            provideHttpClient(),
            provideHttpClientTesting(),
         ],
      });

      service = TestBed.inject(InsightService);
      httpMock = TestBed.inject(HttpTestingController);
   });

   afterEach(() => {
      httpMock.verify();
   });

   it('should be created', () => {
      expect(service).toBeTruthy();
   });

   it('should return kind "ready" with insights array when HTTP 200 is received', () => {
      service.getInsights('AAPL').subscribe({
         next: (result) => {
            expect(result.kind).toBe('ready');
            if (result.kind === 'ready') {
               expect(result.insights).toEqual([mockInsight]);
            }
         },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/insights/AAPL`);
      expect(req.request.method).toBe('GET');
      req.flush([mockInsight], { status: 200, statusText: 'OK' });
   });

   it('should return kind "pending" with message when HTTP 202 is received', () => {
      service.getInsights('AAPL').subscribe({
         next: (result) => {
            expect(result.kind).toBe('pending');
            if (result.kind === 'pending') {
               expect(result.message).toBe('Ingestion en cours...');
            }
         },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/insights/AAPL`);
      expect(req.request.method).toBe('GET');
      req.flush(
         { status: 'INGESTION_PENDING', message: 'Ingestion en cours...' },
         { status: 202, statusText: 'Accepted' },
      );
   });

   it('should propagate 404 error when ticker is unknown to SEC', () => {
      service.getInsights('UNKNOWN').subscribe({
         next: () => expect.unreachable('Should have failed'),
         error: (error) => {
            expect(error.status).toBe(404);
         },
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/insights/UNKNOWN`);
      req.flush(
         { title: 'Not Found', detail: 'Ticker unknown' },
         { status: 404, statusText: 'Not Found' },
      );
   });

   it('should retrieve status via getStatus', () => {
      service.getStatus('AAPL').subscribe({
         next: (status) => {
            expect(status).toEqual(mockStatus);
         },
      });

      const req = httpMock.expectOne(
         `${environment.apiUrl}/insights/AAPL/status`,
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockStatus);
   });

   it('should convert ticker to uppercase in request URLs', () => {
      service.getInsights('aapl').subscribe();
      const req1 = httpMock.expectOne(`${environment.apiUrl}/insights/AAPL`);
      req1.flush([]);

      service.getStatus('msft').subscribe();
      const req2 = httpMock.expectOne(
         `${environment.apiUrl}/insights/MSFT/status`,
      );
      req2.flush(mockStatus);
   });
});
