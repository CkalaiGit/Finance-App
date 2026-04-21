import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { KeycloakService } from './keycloak.service';
import { beforeEach, describe, expect, it, vi } from 'vitest';

describe('AuthService', () => {
  let service: AuthService;
  let keycloakServiceMock: any;

  const mockKeycloakInstance = {
    authenticated: false,
    tokenParsed: null,
    realmAccess: { roles: [] }
  };

  beforeEach(() => {
    keycloakServiceMock = {
      instance: mockKeycloakInstance
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: KeycloakService, useValue: keycloakServiceMock }
      ]
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
});
