import {
   ChangeDetectionStrategy,
   ChangeDetectorRef,
   Component,
   OnInit,
   inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { WatchlistService } from '../../../services/watchlist.service';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { AuthStoreService } from '../../../auth/auth-store.service';
import { UserProfile } from '../../../models/user.model';
import { FullMetrics } from '../../../models/analysis.model';
import { ApiError } from '../../../models/api-error.model';

export interface WatchlistItem {
   ticker: string;
   metrics: FullMetrics | null;
}

@Component({
   selector: 'app-watchlist',
   standalone: true,
   imports: [CommonModule, FormsModule, RouterModule],
   templateUrl: './watchlist.component.html',
   styleUrls: ['./watchlist.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WatchlistComponent implements OnInit {
   private readonly watchlistService = inject(WatchlistService);
   private readonly analysisService = inject(FinancialAnalysisService);
   private readonly authStoreService = inject(AuthStoreService);
   private readonly router = inject(Router);
   private readonly cdr = inject(ChangeDetectorRef);

   readonly user$: Observable<UserProfile | null> =
      this.authStoreService.user$;

   activeTab: 'growth' | 'value' | 'quality' = 'growth';

   items: WatchlistItem[] = [];
   newTicker = '';
   isLoading = true;
   isSubmitting = false;
   errorMessage: string | null = null;

   ngOnInit(): void {
      this.loadWatchlist();
   }

   loadWatchlist(): void {
      this.isLoading = true;
      this.errorMessage = null;
      this.cdr.markForCheck();

      this.watchlistService
         .getWatchlist()
         .pipe(
            switchMap((response) => {
               const tickers = response.tickers || [];
               if (tickers.length === 0) {
                  return of([]);
               }

               // Paralléliser l'appel aux métriques pour chaque ticker
               const metricsRequests$ = tickers.map((ticker) =>
                  this.analysisService.getMetrics(ticker).pipe(
                     catchError((err) => {
                        console.error(
                           `Erreur récupération métriques pour ${ticker}:`,
                           err,
                        );
                        return of(null);
                     }),
                  ),
               );

               return forkJoin(metricsRequests$).pipe(
                  switchMap((metricsArray) => {
                     const watchListItems: WatchlistItem[] = tickers.map(
                        (ticker, index) => ({
                           ticker,
                           metrics: metricsArray[index],
                        }),
                     );
                     return of(watchListItems);
                  }),
               );
            }),
         )
         .subscribe({
            next: (items) => {
               this.items = items;
               this.isLoading = false;
               this.cdr.markForCheck();
            },
            error: (err: ApiError) => {
               this.errorMessage =
                  err.detail || 'Impossible de charger votre watchlist.';
               this.isLoading = false;
               this.cdr.markForCheck();
            },
         });
   }

   switchTab(tab: 'growth' | 'value' | 'quality'): void {
      this.activeTab = tab;
   }

   onTickerInput(value: string): void {
      this.newTicker = value.toUpperCase();
      this.errorMessage = null;
   }

   addTicker(): void {
      const tickerToAdd = this.newTicker.trim().toUpperCase();

      if (!tickerToAdd) {
         this.errorMessage = 'Veuillez saisir un symbole boursier valide.';
         return;
      }

      if (this.items.some((item) => item.ticker === tickerToAdd)) {
         this.errorMessage = `Le ticker ${tickerToAdd} est déjà présent dans votre watchlist.`;
         return;
      }

      this.isSubmitting = true;
      this.errorMessage = null;
      this.cdr.markForCheck();

      this.watchlistService.addTicker(tickerToAdd).subscribe({
         next: () => {
            this.newTicker = '';
            this.isSubmitting = false;
            // Recharger la watchlist complète avec les métriques
            this.loadWatchlist();
         },
         error: (err: ApiError) => {
            this.errorMessage =
               err.detail || "Erreur lors de l'ajout du ticker.";
            this.isSubmitting = false;
            this.cdr.markForCheck();
         },
      });
   }

   removeTicker(event: Event, ticker: string): void {
      event.stopPropagation();
      this.errorMessage = null;

      // Optimistic update
      const previousItems = [...this.items];
      this.items = this.items.filter((item) => item.ticker !== ticker);
      this.cdr.markForCheck();

      this.watchlistService.removeTicker(ticker).subscribe({
         error: (err: ApiError) => {
            this.items = previousItems; // Rollback
            this.errorMessage =
               err.detail || 'Erreur lors de la suppression du ticker.';
            this.cdr.markForCheck();
         },
      });
   }

   navigateToDetails(ticker: string): void {
      this.router.navigate(['/details', ticker]);
   }

   formatPercent(value: number | null | undefined): string {
      if (value === null || value === undefined) return '—';
      return `${(value * 100).toFixed(2)}%`;
   }

   formatRatio(value: number | null | undefined): string {
      if (value === null || value === undefined) return '—';
      return `${value.toFixed(2)}x`;
   }
}
