import { TestBed } from '@angular/core/testing';
import {
   ActivatedRouteSnapshot,
   Router,
   RouterStateSnapshot,
   UrlTree,
   provideRouter,
} from '@angular/router';
import { of, firstValueFrom, isObservable, Observable } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { authGuard, roleGuard } from './auth.guards';
import { AuthStoreService } from './auth-store.service';
import { AuthService } from './auth.service';

describe('Auth Guards', () => {
   let authServiceMock: {
      login: ReturnType<typeof vi.fn>;
   };
   let authStoreMock: {
      isLoggedIn$: Observable<boolean>;
      hasRole: ReturnType<typeof vi.fn>;
      login: ReturnType<typeof vi.fn>;
   };
   let router: Router;

   const mockRoute = {} as unknown as ActivatedRouteSnapshot;
   const stateFor = (url: string): RouterStateSnapshot =>
      ({ url }) as unknown as RouterStateSnapshot;

   beforeEach(() => {
      authServiceMock = {
         login: vi.fn(),
      };
      authStoreMock = {
         isLoggedIn$: of(true),
         hasRole: vi.fn(),
         login: vi.fn(),
      };

      TestBed.configureTestingModule({
         providers: [
            provideRouter([]),
            { provide: AuthService, useValue: authServiceMock },
            { provide: AuthStoreService, useValue: authStoreMock },
         ],
      });

      router = TestBed.inject(Router);
   });

   describe('authGuard', () => {
      it('should allow navigation when user is logged in', async () => {
         authStoreMock.isLoggedIn$ = of(true);

         const result = TestBed.runInInjectionContext(() =>
            authGuard(mockRoute, stateFor('/details/NVDA')),
         );

         const allowed = isObservable(result)
            ? await firstValueFrom(result)
            : result;
         expect(allowed).toBe(true);
         expect(authServiceMock.login).not.toHaveBeenCalled();
         expect(authStoreMock.login).not.toHaveBeenCalled();
      });

      it('should redirect to /home with returnUrl, without triggering login, when user is not logged in', async () => {
         authStoreMock.isLoggedIn$ = of(false);

         const result = TestBed.runInInjectionContext(() =>
            authGuard(mockRoute, stateFor('/details/NVDA')),
         );

         const outcome = isObservable(result)
            ? await firstValueFrom(result)
            : result;
         expect(outcome).toBeInstanceOf(UrlTree);
         const tree = outcome as UrlTree;
         expect(tree.queryParams['returnUrl']).toBe('/details/NVDA');
         expect(router.serializeUrl(tree)).toBe(
            '/home?returnUrl=%2Fdetails%2FNVDA',
         );
         expect(authServiceMock.login).not.toHaveBeenCalled();
         expect(authStoreMock.login).not.toHaveBeenCalled();
      });

      it('should carry the requested url for /watchlist as well', async () => {
         authStoreMock.isLoggedIn$ = of(false);

         const result = TestBed.runInInjectionContext(() =>
            authGuard(mockRoute, stateFor('/watchlist')),
         );

         const outcome = isObservable(result)
            ? await firstValueFrom(result)
            : result;
         expect(router.serializeUrl(outcome as UrlTree)).toBe(
            '/home?returnUrl=%2Fwatchlist',
         );
         expect(authServiceMock.login).not.toHaveBeenCalled();
      });
   });

   describe('roleGuard', () => {
      it('should allow navigation when user has required role', () => {
         authStoreMock.hasRole.mockReturnValue(true);

         const guard = roleGuard('PREMIUM');
         const result = TestBed.runInInjectionContext(() =>
            guard(mockRoute, stateFor('/watchlist')),
         );

         expect(result).toBe(true);
         expect(authStoreMock.hasRole).toHaveBeenCalledWith('PREMIUM');
      });

      it('should redirect to /home when user lacks required role', () => {
         authStoreMock.hasRole.mockReturnValue(false);

         const guard = roleGuard('PREMIUM');
         const result = TestBed.runInInjectionContext(() =>
            guard(mockRoute, stateFor('/watchlist')),
         );

         expect(authStoreMock.hasRole).toHaveBeenCalledWith('PREMIUM');
         expect(result).toBeInstanceOf(UrlTree);
         expect(router.serializeUrl(result as UrlTree)).toBe('/home');
      });
   });
});
