import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HeaderComponent } from './header.component';
import { AuthService } from '../../../auth/auth.service';
import { AuthStoreService } from '../../../auth/auth-store.service';
import { KeycloakService } from '../../../auth/keycloak.service';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

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
      imports: [HeaderComponent],
      providers: [
        { provide: KeycloakService, useValue: keycloakServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: AuthStoreService, useValue: authStoreMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('doit afficher le nom de l utilisateur connecté', () => {
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.username')?.textContent).toContain('Alice');
  });

  it('ne doit pas afficher le nom si l utilisateur est déconnecté', () => {
    authStoreMock.user$ = of(null as any);
    authStoreMock.isLoggedIn$ = of(false);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const loginBtn = compiled.querySelector('.btn-login');
    const userName = compiled.querySelector('.username');

    expect(loginBtn).toBeTruthy(); // Le bouton doit être là
    expect(userName).toBeNull()
  });
});