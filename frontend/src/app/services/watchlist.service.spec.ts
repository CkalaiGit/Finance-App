import { TestBed } from '@angular/core/testing';
import {
   provideHttpClientTesting,
   HttpTestingController,
} from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { WatchlistService } from './watchlist.service';
import {
   WatchlistResponse,
   TickerExistsResponse,
} from '../models/watchlist.model';
import { environment } from '../../environments/environment';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { ApiError } from '../models/api-error.model';

describe('WatchlistService', () => {
   let service: WatchlistService;
   let httpMock: HttpTestingController;

   const baseUrl = `${environment.apiUrl}/watchlist`;

   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            WatchlistService,
            provideHttpClient(withInterceptors([errorInterceptor])),
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

   it('should let an ApiError returned by HttpClient bubble up with its detail', () => {
      const problemDetail = {
         type: 'about:blank',
         title: 'Bad Request',
         status: 400,
         detail: 'Ticker déjà présent dans la watchlist',
         instance: `${baseUrl}/AAPL`,
      };

      service.addTicker('AAPL').subscribe({
         next: () => expect.unreachable('Should have failed'),
         error: (err: ApiError) => {
            expect(err.status).toBe(400);
            expect(err.title).toBe('Bad Request');
            expect(err.detail).toBe('Ticker déjà présent dans la watchlist');
         },
      });

      const req = httpMock.expectOne(`${baseUrl}/AAPL`);
      req.flush(problemDetail, { status: 400, statusText: 'Bad Request' });
   });
});
