import { TestBed } from '@angular/core/testing';
import {
   HttpClient,
   provideHttpClient,
   withInterceptors,
} from '@angular/common/http';
import {
   HttpTestingController,
   provideHttpClientTesting,
} from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { errorInterceptor } from './error.interceptor';
import { ApiError } from '../models/api-error.model';

describe('errorInterceptor', () => {
   let http: HttpClient;
   let httpMock: HttpTestingController;

   beforeEach(() => {
      TestBed.configureTestingModule({
         providers: [
            provideHttpClient(withInterceptors([errorInterceptor])),
            provideHttpClientTesting(),
         ],
      });

      http = TestBed.inject(HttpClient);
      httpMock = TestBed.inject(HttpTestingController);
   });

   afterEach(() => {
      httpMock.verify();
   });

   it('should pass through successful requests', () => {
      const mockData = { message: 'success' };

      http.get('/api/test').subscribe({
         next: (res) => {
            expect(res).toEqual(mockData);
         },
      });

      const req = httpMock.expectOne('/api/test');
      req.flush(mockData);
   });

   it('should convert RFC 7807 ProblemDetail response into ApiError', () => {
      const problemDetail = {
         type: 'about:blank',
         title: 'Not Found',
         status: 404,
         detail: 'Company AAPL not found',
         instance: '/api/v1/companies/AAPL',
      };

      http.get('/api/v1/companies/AAPL').subscribe({
         next: () => expect.unreachable('Should fail'),
         error: (err: ApiError) => {
            expect(err.status).toBe(404);
            expect(err.title).toBe('Not Found');
            expect(err.detail).toBe('Company AAPL not found');
         },
      });

      const req = httpMock.expectOne('/api/v1/companies/AAPL');
      req.flush(problemDetail, { status: 404, statusText: 'Not Found' });
   });

   it('should fallback to statusText and message when problem detail fields are missing', () => {
      http.get('/api/v1/error').subscribe({
         next: () => expect.unreachable('Should fail'),
         error: (err: ApiError) => {
            expect(err.status).toBe(500);
            expect(err.title).toBe('Server Error');
            expect(err.detail).toBe('Database connection failed');
         },
      });

      const req = httpMock.expectOne('/api/v1/error');
      req.flush('Database connection failed', {
         status: 500,
         statusText: 'Server Error',
      });
   });
});
