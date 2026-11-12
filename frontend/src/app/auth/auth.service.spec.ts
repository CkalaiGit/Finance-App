import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { KeycloakService } from './keycloak.service';
import { beforeEach, describe, expect, it, vi } from 'vitest';

describe('AuthService', () => {
   let service: AuthService;
   let mockKeycloakInstance: {
      authenticated: boolean;
      tokenParsed: null;
      realmAccess: { roles: string[] };
      login: ReturnType<typeof vi.fn>;
      logout: ReturnType<typeof vi.fn>;
   };

   beforeEach(() => {
      mockKeycloakInstance = {
         authenticated: false,
         tokenParsed: null,
         realmAccess: { roles: [] },
         login: vi.fn(),
         logout: vi.fn(),
      };

      TestBed.configureTestingModule({
         providers: [
            AuthService,
            {
               provide: KeycloakService,
               useValue: { instance: mockKeycloakInstance },
            },
         ],
      });

      service = TestBed.inject(AuthService);
   });

   it('should be created', () => {
      expect(service).toBeTruthy();
   });

   it('should return false for isLoggedIn when not authenticated', () => {
      expect(service.isLoggedIn).toBe(false);
   });

   it('should return null for getUserProfile when not logged in', () => {
      expect(service.getUserProfile()).toBeNull();
   });

   it('should call keycloak login without options when no redirectUri is given', () => {
      service.login();

      expect(mockKeycloakInstance.login).toHaveBeenCalledTimes(1);
      expect(mockKeycloakInstance.login).toHaveBeenCalledWith();
   });

   it('should forward redirectUri to keycloak login', () => {
      const redirectUri = `${window.location.origin}/details/NVDA`;

      service.login(redirectUri);

      expect(mockKeycloakInstance.login).toHaveBeenCalledWith({ redirectUri });
   });

   it('should logout with a redirect to /home', () => {
      service.logout();

      expect(mockKeycloakInstance.logout).toHaveBeenCalledWith({
         redirectUri: `${window.location.origin}/home`,
      });
   });
});
