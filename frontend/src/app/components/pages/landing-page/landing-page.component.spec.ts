import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
   ActivatedRoute,
   ParamMap,
   Router,
   convertToParamMap,
} from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';

import {
   LandingPageComponent,
   sanitizeReturnUrl,
} from './landing-page.component';
import { AuthStoreService } from '../../../auth/auth-store.service';

const DANGEROUS_RETURN_URLS = [
   '//evil.com',
   'https://evil.com',
   'javascript:alert(1)',
   '/details/NVDA?next=https://evil.com',
   '/\\evil.com',
   'details/NVDA',
];

describe('sanitizeReturnUrl', () => {
   it('should accept internal paths', () => {
      expect(sanitizeReturnUrl('/details/NVDA')).toBe('/details/NVDA');
      expect(sanitizeReturnUrl('/watchlist')).toBe('/watchlist');
   });

   it('should ignore missing values', () => {
      expect(sanitizeReturnUrl(null)).toBeNull();
      expect(sanitizeReturnUrl(undefined)).toBeNull();
      expect(sanitizeReturnUrl('')).toBeNull();
   });

   it.each(DANGEROUS_RETURN_URLS)('should reject %s', (url) => {
      expect(sanitizeReturnUrl(url)).toBeNull();
   });
});

describe('LandingPageComponent', () => {
   let fixture: ComponentFixture<LandingPageComponent>;
   let queryParamMap$: BehaviorSubject<ParamMap>;
   let isLoggedIn$: BehaviorSubject<boolean>;
   let authStoreMock: {
      isLoggedIn$: BehaviorSubject<boolean>;
      login: ReturnType<typeof vi.fn>;
   };
   let routerMock: {
      navigate: ReturnType<typeof vi.fn>;
      navigateByUrl: ReturnType<typeof vi.fn>;
   };

   const paramsFor = (returnUrl?: string): ParamMap =>
      convertToParamMap(returnUrl === undefined ? {} : { returnUrl });

   const createComponent = (options: {
      returnUrl?: string;
      loggedIn?: boolean;
   }): LandingPageComponent => {
      queryParamMap$.next(paramsFor(options.returnUrl));
      isLoggedIn$.next(options.loggedIn ?? false);
      fixture = TestBed.createComponent(LandingPageComponent);
      fixture.detectChanges();
      return fixture.componentInstance;
   };

   const query = (testId: string): HTMLElement | null =>
      (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>(
         `[data-testid="${testId}"]`,
      );

   beforeEach(async () => {
      queryParamMap$ = new BehaviorSubject<ParamMap>(paramsFor());
      isLoggedIn$ = new BehaviorSubject<boolean>(false);
      authStoreMock = { isLoggedIn$, login: vi.fn() };
      routerMock = {
         navigate: vi.fn().mockResolvedValue(true),
         navigateByUrl: vi.fn().mockResolvedValue(true),
      };

      await TestBed.configureTestingModule({
         imports: [LandingPageComponent],
         providers: [
            { provide: AuthStoreService, useValue: authStoreMock },
            { provide: Router, useValue: routerMock },
            {
               provide: ActivatedRoute,
               useValue: { queryParamMap: queryParamMap$.asObservable() },
            },
         ],
      }).compileComponents();
   });

   it('should create', () => {
      expect(createComponent({})).toBeTruthy();
   });

   describe('sans returnUrl', () => {
      it('should not display the login-required banner', () => {
         createComponent({});

         expect(query('login-required')).toBeNull();
         expect(authStoreMock.login).not.toHaveBeenCalled();
         expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
      });

      it('should not navigate anywhere when the user is logged in', () => {
         createComponent({ loggedIn: true });

         expect(query('login-required')).toBeNull();
         expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
      });
   });

   describe('avec returnUrl valide et non connecté', () => {
      it('should display the banner without opening Keycloak', () => {
         createComponent({ returnUrl: '/details/NVDA' });

         const banner = query('login-required');
         expect(banner).not.toBeNull();
         expect(banner?.textContent).toContain(
            'Connectez-vous pour consulter cette page',
         );
         expect(query('login-required-button')?.textContent).toContain(
            'Se connecter',
         );
         expect(authStoreMock.login).not.toHaveBeenCalled();
         expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
      });

      it('should call login with the requested page as redirectUri on click', () => {
         createComponent({ returnUrl: '/details/NVDA' });

         query('login-required-button')?.click();

         expect(authStoreMock.login).toHaveBeenCalledTimes(1);
         expect(authStoreMock.login).toHaveBeenCalledWith(
            `${window.location.origin}/details/NVDA`,
         );
      });

      it('should display the banner when returnUrl arrives while the page is already shown', () => {
         createComponent({});
         expect(query('login-required')).toBeNull();

         queryParamMap$.next(paramsFor('/watchlist'));
         fixture.detectChanges();

         expect(query('login-required')).not.toBeNull();
         query('login-required-button')?.click();
         expect(authStoreMock.login).toHaveBeenCalledWith(
            `${window.location.origin}/watchlist`,
         );
      });
   });

   describe('avec returnUrl dangereux', () => {
      it.each(DANGEROUS_RETURN_URLS)(
         'should ignore %s when not logged in',
         (returnUrl) => {
            createComponent({ returnUrl });

            expect(query('login-required')).toBeNull();
            expect(authStoreMock.login).not.toHaveBeenCalled();
            expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
         },
      );

      it.each(DANGEROUS_RETURN_URLS)(
         'should not navigate to %s when logged in',
         (returnUrl) => {
            createComponent({ returnUrl, loggedIn: true });

            expect(routerMock.navigateByUrl).not.toHaveBeenCalled();
         },
      );

      it('should fall back to /home as redirectUri when returnUrl is invalid', () => {
         const component = createComponent({ returnUrl: '//evil.com' });

         component.onLogin();

         expect(authStoreMock.login).toHaveBeenCalledWith(
            `${window.location.origin}/home`,
         );
      });
   });

   describe('connecté avec returnUrl', () => {
      it('should navigate straight to returnUrl and show no banner', () => {
         createComponent({ returnUrl: '/details/NVDA', loggedIn: true });

         expect(routerMock.navigateByUrl).toHaveBeenCalledTimes(1);
         expect(routerMock.navigateByUrl).toHaveBeenCalledWith(
            '/details/NVDA',
            { replaceUrl: true },
         );
         expect(query('login-required')).toBeNull();
         expect(authStoreMock.login).not.toHaveBeenCalled();
      });
   });

   describe('recherche', () => {
      it('should still navigate to /details/:ticker (the guard handles anonymous visitors)', () => {
         const component = createComponent({});
         component.searchTerm = 'nvda';
         const form = document.createElement('form');
         const event = { preventDefault: vi.fn(), target: form };

         component.onSearch(event as unknown as Event);

         expect(routerMock.navigate).toHaveBeenCalledWith(['/details', 'NVDA']);
         expect(authStoreMock.login).not.toHaveBeenCalled();
      });
   });
});
