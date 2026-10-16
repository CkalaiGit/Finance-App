import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HeaderComponent } from './header.component';
import { AuthService } from '../../../auth/auth.service';
import { AuthStoreService } from '../../../auth/auth-store.service';
import { KeycloakService } from '../../../auth/keycloak.service';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { UserProfile } from '../../../models/user.model';

describe('HeaderComponent', () => {
   let component: HeaderComponent;
   let fixture: ComponentFixture<HeaderComponent>;
   let userSubject: BehaviorSubject<UserProfile | null>;
   let isLoggedInSubject: BehaviorSubject<boolean>;

   const mockKeycloakInstance = {
      authenticated: true,
      tokenParsed: { preferred_username: 'Alice' },
      realmAccess: { roles: ['ADMIN'] },
   };

   const keycloakServiceMock = {
      instance: mockKeycloakInstance,
   };

   const authServiceMock = {
      login: vi.fn(),
      logout: vi.fn(),
      isLoggedIn: true,
   };

   const authStoreMock = {
      get user$() {
         return userSubject.asObservable();
      },
      get isLoggedIn$() {
         return isLoggedInSubject.asObservable();
      },
   };

   const activatedRouteMock = {
      snapshot: { params: {}, data: {} },
   };

   beforeEach(async () => {
      userSubject = new BehaviorSubject<UserProfile | null>({
         username: 'Alice',
         email: 'alice@example.com',
         roles: ['ADMIN'],
      });
      isLoggedInSubject = new BehaviorSubject(true);

      await TestBed.configureTestingModule({
         imports: [HeaderComponent],
         providers: [
            { provide: KeycloakService, useValue: keycloakServiceMock },
            { provide: AuthService, useValue: authServiceMock },
            { provide: AuthStoreService, useValue: authStoreMock },
            { provide: ActivatedRoute, useValue: activatedRouteMock },
         ],
      }).compileComponents();

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

      expect(compiled.querySelector('.username')?.textContent).toContain(
         'Alice',
      );
   });

   it('ne doit pas afficher le nom si l utilisateur est déconnecté', () => {
      userSubject.next(null);
      isLoggedInSubject.next(false);

      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const loginBtn = compiled.querySelector('.btn-login');
      const userName = compiled.querySelector('.username');

      expect(loginBtn).toBeTruthy(); // Le bouton doit être là
      expect(userName).toBeNull();
   });
});
