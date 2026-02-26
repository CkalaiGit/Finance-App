import { ApplicationConfig, provideZoneChangeDetection, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './auth/auth.interceptor';
import { KeycloakService } from './auth/keycloak.service';

export const appConfig: ApplicationConfig = {
  providers: [
    // Optimisation des performances Angular
    provideZoneChangeDetection({ eventCoalescing: true }),
    
    provideRouter(routes),
    
    // Configuration du client HTTP avec ton intercepteur
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),

    // INITIALISATION CRITIQUE : 
    // On lance Keycloak AVANT que l'app ne démarre
    provideAppInitializer(() => {
      const keycloakService = inject<KeycloakService>(KeycloakService);
      return keycloakService.init();
    })
  ]
};