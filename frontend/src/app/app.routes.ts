import { Routes } from '@angular/router';
import { LandingPageComponent } from './components/pages/landing-page/landing-page.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    component: LandingPageComponent,
    title: 'FinanceApp - Accueil'
  },
  // Routes futures
  // {
  //   path: 'watchlist',
  //   loadComponent: () => import('./components/pages/watchlist/watchlist.component')
  //     .then(m => m.WatchlistComponent),
  //   title: 'FinanceApp - Watchlist'
  // },
  // {
  //   path: 'about',
  //   loadComponent: () => import('./components/pages/about/about.component')
  //     .then(m => m.AboutComponent),
  //   title: 'FinanceApp - À propos'
  // },
  {
    path: 'details/:ticker',
    loadComponent: () => import('./components/pages/company-details/company-details.component')
      .then(m => m.CompanyDetailsComponent),
    title: 'FinanceApp - Détails Entreprise'
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];