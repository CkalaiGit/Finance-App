import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from './auth/auth.service';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div style="padding: 20px; font-family: sans-serif;">
      <h1>Finance App - Module 1</h1>
      
      <div *ngIf="!auth.isLoggedIn">
        <button (click)="auth.login()">Se connecter avec Keycloak</button>
      </div>

      <div *ngIf="auth.isLoggedIn">
        <p>Bonjour <strong>{{ auth.username }}</strong> !</p>
        <button (click)="auth.logout()">Déconnexion</button>
      </div>
    </div>
  `
})
export class AppComponent {
  public auth = inject(AuthService);
}