import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { KeycloakService } from './auth/keycloak.service';
import { AuthService } from './auth/auth.service';
import { AuthStoreService } from './auth/auth-store.service';
import { ActivatedRoute } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  const mockKeycloakInstance = {
    authenticated: true,
    tokenParsed: { preferred_username: 'Alice' },
    realmAccess: { roles: ['ADMIN'] }
  };

  const keycloakServiceMock = {
    instance: mockKeycloakInstance
  };

  const authServiceMock = {
    login: vi.fn(),
    logout: vi.fn(),
    isLoggedIn: true
  };

  const authStoreMock = {
    user$: of({ username: 'Alice', roles: ['ADMIN'] }),
    isLoggedIn$: of(true)
  };

  const activatedRouteMock = {
    snapshot: { params: {}, data: {} }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: KeycloakService, useValue: keycloakServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: AuthStoreService, useValue: authStoreMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
