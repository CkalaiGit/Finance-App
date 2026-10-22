import {
   ChangeDetectionStrategy,
   Component,
   inject,
} from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, Subject, combineLatest, forkJoin, merge, of } from 'rxjs';
import {
   catchError,
   distinctUntilChanged,
   filter,
   map,
   shareReplay,
   startWith,
   switchMap,
   take,
   tap,
} from 'rxjs/operators';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { CompanyService } from '../../../services/company.service';
import { WatchlistService } from '../../../services/watchlist.service';
import { FullMetrics } from '../../../models/analysis.model';
import { CompanyProfileResponse } from '../../../models/company.model';
import { ApiError } from '../../../models/api-error.model';
import { WatchlistResponse } from '../../../models/watchlist.model';

export interface WatchlistStatus {
   inWatchlist: boolean;
   loading: boolean;
   error: ApiError | null;
}

interface WatchlistOverride {
   ticker: string;
   status: WatchlistStatus;
}

export interface CompanyDetailsState {
   loading: boolean;
   error: ApiError | null;
   profileError: ApiError | null;
   metricsError: ApiError | null;
   companyProfile: CompanyProfileResponse | null;
   metrics: FullMetrics | null;
   watchlist: WatchlistStatus;
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

   activeTab: 'growth' | 'value' | 'quality' = 'growth';

   private readonly watchlistOverrides$ = new Subject<WatchlistOverride>();

   readonly state$: Observable<CompanyDetailsState> = this.route.paramMap.pipe(
      map((params) => params.get('ticker')?.trim().toUpperCase() || ''),
      distinctUntilChanged(),
      switchMap((ticker) => {
         if (!ticker) {
            return of<CompanyDetailsState>({
               loading: false,
               error: null,
               profileError: null,
               metricsError: null,
               companyProfile: null,
               metrics: null,
               watchlist: {
                  inWatchlist: false,
                  loading: false,
                  error: null,
               },
            });
         }

         const initialWatchlist$: Observable<WatchlistStatus> = this.watchlistService
            .checkExists(ticker)
            .pipe(
               map((res) => ({
                  inWatchlist: res.exists,
                  loading: false,
                  error: null as ApiError | null,
               })),
               catchError((err: unknown) =>
                  of({
                     inWatchlist: false,
                     loading: false,
                     error: this.toApiError(
                        err,
                        'Impossible de vérifier la présence dans la watchlist.',
                     ),
                  }),
               ),
            );

         const watchlist$: Observable<WatchlistStatus> = merge(
            initialWatchlist$,
            this.watchlistOverrides$.pipe(
               filter((override) => override.ticker === ticker),
               map((override) => override.status),
            ),
         );

         return combineLatest({
            data: forkJoin({
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
            }),
            watchlist: watchlist$,
         }).pipe(
            map(({ data, watchlist }) => ({
               loading: false,
               error: data.profileResult.error,
               profileError: data.profileResult.error,
               metricsError: data.metricsResult.error,
               companyProfile: data.profileResult.companyProfile,
               metrics: data.metricsResult.metrics,
               watchlist,
            })),
            startWith<CompanyDetailsState>({
               loading: true,
               error: null,
               profileError: null,
               metricsError: null,
               companyProfile: null,
               metrics: null,
               watchlist: {
                  inWatchlist: false,
                  loading: true,
                  error: null,
               },
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

   readonly watchlist$: Observable<WatchlistStatus> = this.state$.pipe(
      map((s) => s.watchlist),
   );

   readonly isInWatchlist$: Observable<boolean> = this.watchlist$.pipe(
      map((w) => w.inWatchlist),
   );

   readonly isTogglingWatchlist$: Observable<boolean> = this.watchlist$.pipe(
      map((w) => w.loading),
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

   toggleWatchlist(
      ticker?: string,
      currentlyInWatchlist?: boolean,
   ): Observable<WatchlistResponse | null> {
      let targetTicker = ticker;
      let targetInWatchlist = currentlyInWatchlist;

      if (!targetTicker) {
         this.state$.pipe(take(1)).subscribe((state) => {
            targetTicker = state.companyProfile?.symbol || '';
            targetInWatchlist = state.watchlist.inWatchlist;
         });
      }

      if (!targetTicker) {
         return of(null);
      }

      const inWatchlist = !!targetInWatchlist;
      const cleanTicker = targetTicker;

      this.watchlistOverrides$.next({
         ticker: cleanTicker,
         status: {
            inWatchlist,
            loading: true,
            error: null,
         },
      });

      const action$ = inWatchlist
         ? this.watchlistService.removeTicker(cleanTicker)
         : this.watchlistService.addTicker(cleanTicker);

      const result$ = action$.pipe(
         tap({
            next: () => {
               this.watchlistOverrides$.next({
                  ticker: cleanTicker,
                  status: {
                     inWatchlist: !inWatchlist,
                     loading: false,
                     error: null,
                  },
               });
            },
            error: (err: unknown) => {
               this.watchlistOverrides$.next({
                  ticker: cleanTicker,
                  status: {
                     inWatchlist,
                     loading: false,
                     error: this.toApiError(
                        err,
                        'Erreur lors de la modification de la watchlist.',
                     ),
                  },
               });
            },
         }),
         shareReplay({ bufferSize: 1, refCount: true }),
      );

      // Déclenche l'action pour les appels depuis les handlers d'événements du template
      result$.pipe(take(1)).subscribe({ error: () => {} });

      return result$;
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
