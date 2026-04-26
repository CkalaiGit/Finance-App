import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router'; // Indispensable pour la navigation

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, FormsModule], // CommonModule utile pour le pipe 'currency'
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.scss']
})
export class LandingPageComponent {
  // Terme de recherche (lié si tu utilises [(ngModel)] dans ton input)
  searchTerm: string = '';

  constructor(
    private readonly router: Router // Injecté pour gérer les clics
  ) {}

  // --- INTERACTIONS UTILISATEUR ---

  /**
   * Déclenchée lors de la soumission du formulaire (touche Entrée ou clic sur la loupe)
   */
  onSearch(event: Event): void {
    event.preventDefault(); // Empêche le rechargement total de la page
    
    // Récupération de la valeur (soit via ngModel, soit directement dans le DOM)
    const input = (event.target as HTMLFormElement).querySelector('input');
    const searchValue = input?.value.trim() || this.searchTerm.trim();

    if (!searchValue) {
      return; // Ne fait rien si la recherche est vide
    }

    console.log('Recherche lancée pour :', searchValue);
    
    // Navigation vers la page de détails de l'entreprise recherchée
    this.router.navigate(['/details', searchValue.toUpperCase()]);
  }


}