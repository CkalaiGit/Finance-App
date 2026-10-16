import { TestBed } from '@angular/core/testing';
import {
   ActivatedRouteSnapshot,
   Router,
   RouterStateSnapshot,
   UrlTree,
} from '@angular/router';
import { of, firstValueFrom, isObservable } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { authGuard, roleGuard } from './auth.guards';
import { AuthStoreService } from './auth-store.service';
import { AuthService } from './auth.service';

describe('Auth Guards', () => {
   let authServiceMock: {
      login: ReturnType<typeof vi.fn>;
   };
   let authStoreMock: {
      isLoggedIn$: ReturnType<typeof of<boolean>>;
      hasRole: ReturnType<typeof vi.fn>;
   };
   let routerMock: {
      createUrlTree: ReturnType<typeof vi.fn>;
   };

   const mockRoute = {} as unknown as ActivatedRouteSnapshot;
   const mockState = {} as unknown as RouterStateSnapshot;

   beforeEach(() => {
      authServiceMock = {
         login: vi.fn(),
      };
      authStoreMock = {
         isLoggedIn$: of(true),
         hasRole: vi.fn(),
      };
      routerMock = {
         createUrlTree: vi.fn((commands: string[]) => ({
            toString: () => `/${commands.join('/')}`,
         } as unknown as UrlTree)),
      };

      TestBed.configureTestingModule({
         providers: [
            { provide: AuthService, useValue: authServiceMock },
            { provide: AuthStoreService, useValue: authStoreMock },
            { provide: Router, useValue: routerMock },
         ],
      });
   });

   describe('authGuard', () => {
      it('should allow navigation when user is logged in', async () => {
         authStoreMock.isLoggedIn$ = of(true);

         const result = TestBed.runInInjectionContext(() =>
            authGuard(mockRoute, mockState),
         );

         const allowed = isObservable(result) ? await firstValueFrom(result) : result;
         expect(allowed).toBe(true);
         expect(authServiceMock.login).not.toHaveBeenCalled();
      });

      it('should block navigation and trigger login when user is not logged in', async () => {
         authStoreMock.isLoggedIn$ = of(false);

         const result = TestBed.runInInjectionContext(() =>
            authGuard(mockRoute, mockState),
         );

         const allowed = isObservable(result) ? await firstValueFrom(result) : result;
         expect(allowed).toBe(false);
         expect(authServiceMock.login).toHaveBeenCalled();
      });
   });

   describe('roleGuard', () => {
      it('should allow navigation when user has required role', () => {
         authStoreMock.hasRole.mockReturnValue(true);

         const guard = roleGuard('PREMIUM');
         const result = TestBed.runInInjectionContext(() =>
            guard(mockRoute, mockState),
         );

         expect(result).toBe(true);
         expect(authStoreMock.hasRole).toHaveBeenCalledWith('PREMIUM');
         expect(routerMock.createUrlTree).not.toHaveBeenCalled();
      });

      it('should redirect to /home when user lacks required role', () => {
         authStoreMock.hasRole.mockReturnValue(false);

         const guard = roleGuard('PREMIUM');
         const result = TestBed.runInInjectionContext(() =>
            guard(mockRoute, mockState),
         );

         expect(authStoreMock.hasRole).toHaveBeenCalledWith('PREMIUM');
         expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/home']);
         expect(result).toBeTruthy();
      });
   });
});
