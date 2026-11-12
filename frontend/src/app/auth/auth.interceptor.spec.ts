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
import { firstValueFrom } from 'rxjs';
import { ApiError } from '../models/api-error.model';
import { UserService } from '../user/user.service';

describe('authInterceptor', () => {
   let http: HttpClient;
   let httpMock: HttpTestingController;
   let authServiceMock: {
      isLoggedIn: boolean;
      getToken: ReturnType<typeof vi.fn>;
      login: ReturnType<typeof vi.fn>;
   };
   let keycloakInstanceMock: {
      updateToken: ReturnType<typeof vi.fn>;
      token?: string;
   };

   beforeEach(() => {
      authServiceMock = {
         isLoggedIn: true,
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

   it('should send Authorization header on /users/me via UserService (regression: URL sans /v1)', async () => {
      TestBed.inject(UserService).getMe().subscribe();

      await Promise.resolve();

      const req = httpMock.expectOne(`${environment.apiUrl}/users/me`);
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

   it('should send the request untouched for an anonymous visitor (no header, no updateToken, no login)', () => {
      authServiceMock.isLoggedIn = false;
      authServiceMock.getToken.mockReturnValue(undefined);

      http.get(`${environment.apiUrl}/companies/AAPL`).subscribe();

      // Requête émise de façon synchrone : aucun renouvellement n'est attendu
      const req = httpMock.expectOne(`${environment.apiUrl}/companies/AAPL`);
      expect(req.request.headers.has('Authorization')).toBe(false);
      expect(keycloakInstanceMock.updateToken).not.toHaveBeenCalled();
      expect(authServiceMock.login).not.toHaveBeenCalled();
      req.flush({});
   });

   it('should not trigger login for an anonymous visitor when the backend answers 401', () => {
      authServiceMock.isLoggedIn = false;

      let status: number | undefined;
      http.get(`${environment.apiUrl}/watchlist`).subscribe({
         error: (err: { status: number }) => (status = err.status),
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/watchlist`);
      req.flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(status).toBe(401);
      expect(keycloakInstanceMock.updateToken).not.toHaveBeenCalled();
      expect(authServiceMock.login).not.toHaveBeenCalled();
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
      try {
         await firstValueFrom(http.get(`${environment.apiUrl}/watchlist`));
         expect.unreachable('Should have failed');
      } catch (err) {
         caughtError = err as ApiError;
      }

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
