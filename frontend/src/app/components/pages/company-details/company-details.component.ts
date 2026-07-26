import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Observable, of } from 'rxjs';
import {
   switchMap,
   catchError,
   map,
   distinctUntilChanged,
} from 'rxjs/operators';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { CompanyService } from '../../../services/company.service';
import { WatchlistService } from '../../../services/watchlist.service';
import { FullMetrics } from '../../../models/analysis.model';
import { CompanyProfileResponse } from '../../../models/company.model';

@Component({
   selector: 'app-company-details',
   standalone: true,
   imports: [CommonModule],
   templateUrl: './company-details.component.html',
   styleUrls: ['./company-details.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompanyDetailsComponent implements OnInit {
   activeTab: 'growth' | 'value' | 'quality' = 'growth';

   metrics$!: Observable<FullMetrics | null>;
   companyProfile$!: Observable<CompanyProfileResponse | null>;

   currentTicker = '';
   isInWatchlist = false;
   isTogglingWatchlist = false;

   constructor(
      private readonly route: ActivatedRoute,
      private readonly analysisService: FinancialAnalysisService,
      private readonly companyService: CompanyService,
      private readonly watchlistService: WatchlistService,
      private readonly cdr: ChangeDetectorRef,
   ) {}

   ngOnInit(): void {
      this.route.paramMap
         .pipe(
            map((params) => params.get('ticker') || ''),
            distinctUntilChanged(),
            switchMap((ticker) => {
               this.currentTicker = ticker;
               this.isInWatchlist = false;
               if (!ticker) {
                  this.metrics$ = of(null);
                  this.companyProfile$ = of(null);
                  return of(null);
               }
               this.metrics$ = this.analysisService
                  .getMetrics(ticker)
                  .pipe(catchError(() => of(null)));
               this.companyProfile$ = this.companyService
                  .getCompanyProfile(ticker)
                  .pipe(catchError(() => of(null)));

               this.checkWatchlistStatus(ticker);
               return of(ticker);
            }),
         )
         .subscribe();
   }

   private checkWatchlistStatus(ticker: string): void {
      this.watchlistService
         .checkExists(ticker)
         .pipe(catchError(() => of({ ticker, exists: false })))
         .subscribe({
            next: (res) => {
               this.isInWatchlist = res.exists;
               this.cdr.markForCheck();
            },
         });
   }

   toggleWatchlist(): void {
      if (!this.currentTicker || this.isTogglingWatchlist) return;

      this.isTogglingWatchlist = true;
      this.cdr.markForCheck();

      const action$ = this.isInWatchlist
         ? this.watchlistService.removeTicker(this.currentTicker)
         : this.watchlistService.addTicker(this.currentTicker);

      action$
         .pipe(
            catchError((err) => {
               console.error('Erreur lors de la modification de la watchlist:', err);
               return of(null);
            }),
         )
         .subscribe({
            next: (res) => {
               if (res) {
                  this.isInWatchlist = !this.isInWatchlist;
               }
               this.isTogglingWatchlist = false;
               this.cdr.markForCheck();
            },
         });
   }

   switchTab(tab: 'growth' | 'value' | 'quality'): void {
      this.activeTab = tab;
   }

   formatPercent(value: number | null | undefined): string {
      if (value === null || value === undefined) return 'N/A';
      return `${(value * 100).toFixed(2)}%`;
   }

   formatRatio(value: number | null | undefined): string {
      if (value === null || value === undefined) return 'N/A';
      return `${value.toFixed(2)}x`;
   }
}

