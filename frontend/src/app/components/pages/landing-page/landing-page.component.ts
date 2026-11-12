import {
   ChangeDetectionStrategy,
   Component,
   computed,
   inject,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, distinctUntilChanged, map } from 'rxjs';
import { AuthStoreService } from '../../../auth/auth-store.service';

const DEFAULT_RETURN_URL = '/home';

/**
 * Valide un `returnUrl` reçu en paramètre de requête (donc non fiable).
 * Seul un chemin interne est accepté : il doit commencer par « / », ne pas
 * commencer par « // » et ne contenir ni « : » ni « \ ». Tout le reste est
 * ignoré (null) pour éviter une redirection ouverte.
 */
export function sanitizeReturnUrl(
   raw: string | null | undefined,
): string | null {
   if (!raw) {
      return null;
   }
   if (!raw.startsWith('/') || raw.startsWith('//')) {
      return null;
   }
   if (raw.includes(':') || raw.includes('\\')) {
      return null;
   }
   return raw;
}

@Component({
   selector: 'app-landing-page',
   standalone: true,
   imports: [FormsModule],
   templateUrl: './landing-page.component.html',
   styleUrls: ['./landing-page.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingPageComponent {
   private readonly router = inject(Router);
   private readonly route = inject(ActivatedRoute);
   private readonly authStore = inject(AuthStoreService);

   // Terme de recherche (lié si tu utilises [(ngModel)] dans ton input)
   searchTerm: string = '';

   /**
    * returnUrl validé, ou null s'il est absent ou dangereux.
    * Observé en continu : le garde peut ramener sur /home alors que la page
    * d'accueil est déjà affichée (seuls les paramètres de requête changent).
    */
   private readonly returnUrl$ = this.route.queryParamMap.pipe(
      map((params) => sanitizeReturnUrl(params.get('returnUrl'))),
      distinctUntilChanged(),
   );

   private readonly returnUrl = toSignal(this.returnUrl$, {
      initialValue: null,
   });
   private readonly isLoggedIn = toSignal(this.authStore.isLoggedIn$, {
      initialValue: false,
   });

   /** Encart « Connectez-vous pour consulter cette page ». */
   readonly showLoginRequired = computed(
      () => this.returnUrl() !== null && !this.isLoggedIn(),
   );

   constructor() {
      // Déjà connecté avec un returnUrl valide : on va directement sur la page.
      combineLatest([this.returnUrl$, this.authStore.isLoggedIn$])
         .pipe(takeUntilDestroyed())
         .subscribe(([returnUrl, isLoggedIn]) => {
            if (isLoggedIn && returnUrl) {
               void this.router.navigateByUrl(returnUrl, { replaceUrl: true });
            }
         });
   }

   // --- INTERACTIONS UTILISATEUR ---

   /**
    * Ouvre Keycloak, avec retour sur la page initialement demandée.
    */
   onLogin(): void {
      const target = this.returnUrl() ?? DEFAULT_RETURN_URL;
      this.authStore.login(window.location.origin + target);
   }

   /**
    * Déclenchée lors de la soumission du formulaire (touche Entrée ou clic sur la loupe)
    */
   onSearch(event: Event): void {
      event.preventDefault();

      const input = (event.target as HTMLFormElement).querySelector('input');
      const searchValue = input?.value.trim() || this.searchTerm.trim();

      if (!searchValue) {
         return;
      }

      this.router.navigate(['/details', searchValue.toUpperCase()]);
   }
}
