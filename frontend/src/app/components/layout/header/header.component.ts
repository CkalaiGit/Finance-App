import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Requis pour le pipe async
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthStoreService } from '../../../auth/auth-store.service'; // Point 1 : Service dédié pour la gestion de l'état d'authentification

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush // Point 3 : Performance max
})
export class HeaderComponent {
  // Point 2.1 : Injection par constructeur obligatoire
  constructor(public readonly authStore: AuthStoreService) {}

 // Injectez votre service d'authentification
  // authStore = inject(AuthStore);

  // État du menu mobile
  mobileMenuOpen = false;

  /**
   * Toggle du menu mobile
   */
  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
    
    // Ajouter/retirer la classe sur le menu
    const navMenu = document.querySelector('.nav-menu');
    if (navMenu) {
      if (this.mobileMenuOpen) {
        navMenu.classList.add('mobile-open');
      } else {
        navMenu.classList.remove('mobile-open');
      }
    }

    // Mettre à jour l'attribut aria-expanded
    const toggleButton = document.querySelector('.mobile-menu-toggle');
    if (toggleButton) {
      toggleButton.setAttribute('aria-expanded', this.mobileMenuOpen.toString());
    }
  }

  /**
   * Fermer le menu mobile lors d'un clic sur un lien
   * (à appeler dans le template si nécessaire)
   */
  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
    const navMenu = document.querySelector('.nav-menu');
    if (navMenu) {
      navMenu.classList.remove('mobile-open');
    }
  }
}