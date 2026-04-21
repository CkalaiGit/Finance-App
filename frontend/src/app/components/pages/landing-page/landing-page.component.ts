import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router'; // Indispensable pour la navigation
import { CompanyService } from '../../../services/company.service';
import { CompanyDisplayDTO } from '../../../models/company.model';
import { Observable, BehaviorSubject, catchError, of } from 'rxjs';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, FormsModule], // CommonModule utile pour le pipe 'currency'
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.scss']
})
export class LandingPageComponent implements OnInit {
  // --- ÉTATS DU COMPOSANT ---

  // État 1 : Le flux de données principal (liste des entreprises)
  companies$!: Observable<CompanyDisplayDTO[]>;
  
  // État 2 : Gestion des erreurs (permet de relancer via le bouton "Réessayer")
  error$ = new BehaviorSubject<string | null>(null);

  // Terme de recherche (lié si tu utilises [(ngModel)] dans ton input)
  searchTerm: string = '';

  constructor(
    private readonly companyService: CompanyService,
    private readonly router: Router // Injecté pour gérer les clics
  ) {}

  ngOnInit(): void {
    this.load();
  }

  /**
   * Charge ou recharge la liste des entreprises vedettes
   */
  load(): void {
    this.error$.next(null); // On réinitialise l'erreur avant de charger
    
    this.companies$ = this.companyService.getFeaturedCompanies().pipe(
      catchError((err) => {
        console.error('Erreur de chargement:', err);
        this.error$.next("Oups ! Impossible de récupérer les données du marché.");
        return of([]); // Retourne un tableau vide pour ne pas casser le flux
      })
    );
  }

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

  /**
   * Déclenchée lors du clic (ou appui sur Entrée) sur une carte entreprise
   */
  onCompanyClick(company: CompanyDisplayDTO): void {
    console.log('Navigation vers les détails de :', company.ticker);
    
    this.router.navigate(['/details', company.ticker]);
  }
}