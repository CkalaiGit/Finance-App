import {
   ChangeDetectionStrategy,
   ChangeDetectorRef,
   Component,
   inject,
} from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import {
   catchError,
   distinctUntilChanged,
   map,
   shareReplay,
   startWith,
   switchMap,
} from 'rxjs/operators';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { CompanyService } from '../../../services/company.service';
import { WatchlistService } from '../../../services/watchlist.service';
import { FullMetrics } from '../../../models/analysis.model';
import { CompanyProfileResponse } from '../../../models/company.model';
import { ApiError } from '../../../models/api-error.model';

export interface CompanyDetailsState {
   loading: boolean;
   error: ApiError | null;
   profileError: ApiError | null;
   metricsError: ApiError | null;
   companyProfile: CompanyProfileResponse | null;
   metrics: FullMetrics | null;
}

@Component({
   selector: 'app-company-details',
   standalone: true,
   imports: [AsyncPipe, CurrencyPipe, RouterLink],
   templateUrl: './company-details.component.html',
   styleUrls: ['./company-details.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompanyDetailsComponent {
   private readonly route = inject(ActivatedRoute);
   private readonly analysisService = inject(FinancialAnalysisService);
   private readonly companyService = inject(CompanyService);
   private readonly watchlistService = inject(WatchlistService);
   private readonly cdr = inject(ChangeDetectorRef);

   activeTab: 'growth' | 'value' | 'quality' = 'growth';

   currentTicker = '';
   isInWatchlist = false;
   isTogglingWatchlist = false;

   readonly state$: Observable<CompanyDetailsState> = this.route.paramMap.pipe(
      map((params) => params.get('ticker')?.trim().toUpperCase() || ''),
      distinctUntilChanged(),
      switchMap((ticker) => {
         this.currentTicker = ticker;
         if (!ticker) {
            this.isInWatchlist = false;
            return of<CompanyDetailsState>({
               loading: false,
               error: null,
               profileError: null,
               metricsError: null,
               companyProfile: null,
               metrics: null,
            });
         }

         this.checkWatchlistStatus(ticker);

         return forkJoin({
            profileResult: this.companyService.getCompanyProfile(ticker).pipe(
               map((companyProfile) => ({
                  companyProfile,
                  error: null as ApiError | null,
               })),
               catchError((err: unknown) =>
                  of({
                     companyProfile: null,
                     error: this.toApiError(
                        err,
                        'Impossible de charger les données de l’entreprise.',
                     ),
                  }),
               ),
            ),
            metricsResult: this.analysisService.getMetrics(ticker).pipe(
               map((metrics) => ({ metrics, error: null as ApiError | null })),
               catchError((err: unknown) =>
                  of({
                     metrics: null,
                     error: this.toApiError(
                        err,
                        'Impossible de charger les métriques financières.',
                     ),
                  }),
               ),
            ),
         }).pipe(
            map(({ profileResult, metricsResult }) => ({
               loading: false,
               error: profileResult.error,
               profileError: profileResult.error,
               metricsError: metricsResult.error,
               companyProfile: profileResult.companyProfile,
               metrics: metricsResult.metrics,
            })),
            startWith<CompanyDetailsState>({
               loading: true,
               error: null,
               profileError: null,
               metricsError: null,
               companyProfile: null,
               metrics: null,
            }),
         );
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
   );

   readonly companyProfile$: Observable<CompanyProfileResponse | null> =
      this.state$.pipe(map((s) => s.companyProfile));

   readonly metrics$: Observable<FullMetrics | null> = this.state$.pipe(
      map((s) => s.metrics),
   );

   readonly loading$: Observable<boolean> = this.state$.pipe(
      map((s) => s.loading),
   );

   readonly error$: Observable<ApiError | null> = this.state$.pipe(
      map((s) => s.error),
   );

   readonly profileError$: Observable<ApiError | null> = this.state$.pipe(
      map((s) => s.profileError),
   );

   readonly metricsError$: Observable<ApiError | null> = this.state$.pipe(
      map((s) => s.metricsError),
   );

   private toApiError(err: unknown, defaultDetail: string): ApiError {
      if (
         err &&
         typeof err === 'object' &&
         'title' in err &&
         'detail' in err
      ) {
         const apiErr = err as ApiError;
         return {
            status: typeof apiErr.status === 'number' ? apiErr.status : 500,
            title: apiErr.title || 'Erreur',
            detail: apiErr.detail || defaultDetail,
         };
      }
      return {
         status: 500,
         title: 'Erreur',
         detail: (err as Error)?.message || defaultDetail,
      };
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
               console.error(
                  'Erreur lors de la modification de la watchlist:',
                  err,
               );
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
