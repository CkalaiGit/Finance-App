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
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { KeycloakService } from './keycloak.service';
import { environment } from '../../environments/environment';
import { ApiError } from '../models/api-error.model';

describe('authInterceptor', () => {
   let http: HttpClient;
   let httpMock: HttpTestingController;
   let authServiceMock: {
      getToken: ReturnType<typeof vi.fn>;
      login: ReturnType<typeof vi.fn>;
   };
   let keycloakInstanceMock: {
      updateToken: ReturnType<typeof vi.fn>;
      token?: string;
   };

   beforeEach(() => {
      authServiceMock = {
         getToken: vi.fn().mockReturnValue('mock-jwt-token'),
         login: vi.fn(),
      };

      keycloakInstanceMock = {
         updateToken: vi.fn().mockReturnValue(Promise.resolve(true)),
         token: 'mock-jwt-token',
      };

      TestBed.configureTestingModule({
         providers: [
            provideHttpClient(withInterceptors([authInterceptor])),
            provideHttpClientTesting(),
            { provide: AuthService, useValue: authServiceMock },
            {
               provide: KeycloakService,
               useValue: { instance: keycloakInstanceMock },
            },
         ],
      });

      http = TestBed.inject(HttpClient);
      httpMock = TestBed.inject(HttpTestingController);
   });

   afterEach(() => {
      httpMock.verify();
   });

   it('should add Authorization header for requests targeting apiUrl', async () => {
      http.get(`${environment.apiUrl}/companies/AAPL`).subscribe();

      await Promise.resolve();

      const req = httpMock.expectOne(`${environment.apiUrl}/companies/AAPL`);
      expect(req.request.headers.has('Authorization')).toBe(true);
      expect(req.request.headers.get('Authorization')).toBe(
         'Bearer mock-jwt-token',
      );
      req.flush({});
   });

   it('should not add Authorization header for external URLs', () => {
      http.get('https://api.external.com/data').subscribe();

      const req = httpMock.expectOne('https://api.external.com/data');
      expect(req.request.headers.has('Authorization')).toBe(false);
      expect(keycloakInstanceMock.updateToken).not.toHaveBeenCalled();
      req.flush({});
   });

   it('should call updateToken(30) before sending request to apiUrl', async () => {
      http.get(`${environment.apiUrl}/watchlist`).subscribe();

      expect(keycloakInstanceMock.updateToken).toHaveBeenCalledWith(30);

      await Promise.resolve();

      const req = httpMock.expectOne(`${environment.apiUrl}/watchlist`);
      expect(req.request.method).toBe('GET');
      req.flush({});
   });

   it('should trigger login and throw ApiError 401 if updateToken fails', async () => {
      keycloakInstanceMock.updateToken.mockReturnValue(
         Promise.reject(new Error('Token refresh failed')),
      );

      let caughtError: ApiError | null = null;
      http.get(`${environment.apiUrl}/watchlist`).subscribe({
         next: () => expect.unreachable('Should have failed'),
         error: (err: ApiError) => {
            caughtError = err;
         },
      });

      await Promise.resolve();

      expect(authServiceMock.login).toHaveBeenCalled();
      expect(caughtError).toBeTruthy();
      expect(caughtError?.status).toBe(401);
      expect(caughtError?.detail).toContain('session');
      httpMock.expectNone(`${environment.apiUrl}/watchlist`);
   });

   it('should call updateToken only once for concurrent parallel requests', async () => {
      let resolvePromise: (value: boolean) => void = () => {};
      const pendingPromise = new Promise<boolean>((resolve) => {
         resolvePromise = resolve;
      });
      keycloakInstanceMock.updateToken.mockReturnValue(pendingPromise);

      // Launch 2 parallel requests
      http.get(`${environment.apiUrl}/res1`).subscribe();
      http.get(`${environment.apiUrl}/res2`).subscribe();

      expect(keycloakInstanceMock.updateToken).toHaveBeenCalledTimes(1);

      // Resolve the renewal
      resolvePromise(true);
      await Promise.resolve();
      await Promise.resolve();

      const req1 = httpMock.expectOne(`${environment.apiUrl}/res1`);
      const req2 = httpMock.expectOne(`${environment.apiUrl}/res2`);
      expect(req1.request.headers.get('Authorization')).toBe(
         'Bearer mock-jwt-token',
      );
      expect(req2.request.headers.get('Authorization')).toBe(
         'Bearer mock-jwt-token',
      );
      req1.flush({});
      req2.flush({});
   });
});
