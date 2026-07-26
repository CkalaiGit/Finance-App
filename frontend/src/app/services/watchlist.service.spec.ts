import { TestBed } from '@angular/core/testing';
import {
   provideHttpClientTesting,
   HttpTestingController,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { WatchlistService } from './watchlist.service';
import {
   WatchlistResponse,
   TickerExistsResponse,
} from '../models/watchlist.model';

describe('WatchlistService', () => {
   let service: WatchlistService;
   let httpMock: HttpTestingController;

   const baseUrl = 'http://localhost:8081/api/v1/watchlist';

   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            WatchlistService,
            provideHttpClient(),
            provideHttpClientTesting(),
         ],
      });

      service = TestBed.inject(WatchlistService);
      httpMock = TestBed.inject(HttpTestingController);
   });

   afterEach(() => {
      httpMock.verify();
   });

   it('should be created', () => {
      expect(service).toBeTruthy();
   });

   it('should fetch watchlist (getWatchlist)', () => {
      const mockResponse: WatchlistResponse = {
         keycloakId: 'user-123',
         tickers: ['AAPL', 'MSFT'],
      };

      service.getWatchlist().subscribe((response) => {
         expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
   });

   it('should add a ticker (addTicker)', () => {
      const mockResponse: WatchlistResponse = {
         keycloakId: 'user-123',
         tickers: ['AAPL', 'NVDA'],
      };

      service.addTicker('nvda').subscribe((response) => {
         expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/NVDA`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
   });

   it('should remove a ticker (removeTicker)', () => {
      const mockResponse: WatchlistResponse = {
         keycloakId: 'user-123',
         tickers: ['AAPL'],
      };

      service.removeTicker('msft').subscribe((response) => {
         expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/MSFT`);
      expect(req.request.method).toBe('DELETE');
      req.flush(mockResponse);
   });

   it('should check if a ticker exists (checkExists)', () => {
      const mockResponse: TickerExistsResponse = {
         ticker: 'AAPL',
         exists: true,
      };

      service.checkExists('aapl').subscribe((response) => {
         expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/AAPL/exists`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
   });

   it('should handle HTTP error gracefully', () => {
      service.getWatchlist().subscribe({
         next: () => expect.fail('Should have failed'),
         error: (error) => {
            expect(error.message).toBeTruthy();
         },
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
   });
});
