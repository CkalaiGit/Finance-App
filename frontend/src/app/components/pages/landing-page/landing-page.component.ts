import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router'; // Indispensable pour la navigation

@Component({
   selector: 'app-landing-page',
   standalone: true,
   imports: [FormsModule], // CommonModule utile pour le pipe 'currency'
   templateUrl: './landing-page.component.html',
   styleUrls: ['./landing-page.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingPageComponent {
   private readonly router = inject(Router);

   // Terme de recherche (lié si tu utilises [(ngModel)] dans ton input)
   searchTerm: string = '';

   // --- INTERACTIONS UTILISATEUR ---

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
