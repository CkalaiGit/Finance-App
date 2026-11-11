import {
   ChangeDetectionStrategy,
   ChangeDetectorRef,
   Component,
   DestroyRef,
   Input,
   inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
   BehaviorSubject,
   Observable,
   Subject,
   combineLatest,
   defer,
   distinctUntilChanged,
   filter,
   map,
   merge,
   of,
   shareReplay,
   startWith,
   switchMap,
   takeUntil,
   takeWhile,
   timer,
} from 'rxjs';
import { catchError } from 'rxjs/operators';
import { InsightService } from '../../../../services/insight.service';
import {
   IngestionStatus,
   Insight,
   InsightsResult,
} from '../../../../models/insight.model';
import { ApiError } from '../../../../models/api-error.model';

export const POLLING_INTERVAL_MS = 5000;
export const POLLING_TIMEOUT_MS = 180000; // 3 minutes

export type InsightsTabStatus =
   | 'idle'
   | 'loading'
   | 'pending'
   | 'partial'
   | 'ready'
   | 'timeout'
   | 'unavailable'
   | 'error';

export interface InsightsTabState {
   status: InsightsTabStatus;
   pendingMessage?: string;
   error?: ApiError;
   insights: Insight[];
   selectedInsight: Insight | null;
}

interface InternalFlowState {
   status: InsightsTabStatus;
   pendingMessage?: string;
   error?: ApiError;
   insights: Insight[];
   selectedAccession: string | null;
}

function extractYear(period: string): number {
   const match = period.match(/\b(19\d{2}|20\d{2})\b/);
   return match ? parseInt(match[1], 10) : 0;
}

function extractQuarter(period: string): number {
   const match = period.match(/Q([1-4])/i);
   return match ? parseInt(match[1], 10) : 0;
}

export function comparePeriods(a: string, b: string): number {
   const yearA = extractYear(a);
   const yearB = extractYear(b);
   if (yearA !== yearB) {
      return yearB - yearA;
   }
   const qA = extractQuarter(a);
   const qB = extractQuarter(b);
   if (qA !== qB) {
      return qB - qA;
   }
   return b.localeCompare(a, undefined, { numeric: true });
}

export function sortInsights(insights: Insight[]): Insight[] {
   return [...insights].sort((a, b) => {
      if (a.formType !== b.formType) {
         return a.formType === '10-K' ? -1 : 1;
      }
      return comparePeriods(a.period, b.period);
   });
}

export function pickDefaultInsight(sorted: Insight[]): Insight | null {
   if (!sorted || sorted.length === 0) return null;
   const first10K = sorted.find((i) => i.formType === '10-K');
   return first10K ?? sorted[0];
}

export function isDemoInsight(insight: Insight | null | undefined): boolean {
   return !!insight?.synthesePerformance?.startsWith('[DÉMO] ');
}

export function cleanDemoPrefix(text: string | null | undefined): string {
   if (!text) return '';
   return text.replace(/^\[DÉMO\]\s*/i, '');
}

function toApiError(err: unknown, defaultDetail: string): ApiError {
   if (err && typeof err === 'object' && 'title' in err && 'detail' in err) {
      const apiErr = err as ApiError;
      return {
         status: typeof apiErr.status === 'number' ? apiErr.status : 500,
         title: apiErr.title || 'Erreur',
         detail: apiErr.detail || defaultDetail,
      };
   }
   if (err && typeof err === 'object' && 'status' in err) {
      const httpErr = err as {
         status?: number;
         error?: { title?: string; detail?: string };
      };
      return {
         status: typeof httpErr.status === 'number' ? httpErr.status : 500,
         title: httpErr.error?.title || 'Erreur',
         detail: httpErr.error?.detail || defaultDetail,
      };
   }
   return {
      status: 500,
      title: 'Erreur',
      detail: (err as Error)?.message || defaultDetail,
   };
}

function isNotFoundError(err: unknown): boolean {
   if (err && typeof err === 'object' && 'status' in err) {
      return (err as { status?: number }).status === 404;
   }
   return false;
}

@Component({
   selector: 'app-insights-tab',
   standalone: true,
   imports: [CommonModule],
   templateUrl: './insights-tab.component.html',
   styleUrls: ['./insights-tab.component.scss'],
   changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InsightsTabComponent {
   private readonly insightService = inject(InsightService);
   private readonly destroyRef = inject(DestroyRef);
   private readonly cdr = inject(ChangeDetectorRef);

   private readonly ticker$ = new BehaviorSubject<string>('');
   private readonly active$ = new BehaviorSubject<boolean>(false);
   private readonly retry$ = new Subject<void>();
   private readonly selectedAccessionOverride$ = new BehaviorSubject<
      string | null
   >(null);

   private _ticker = '';
   private _active = false;

   @Input()
   set ticker(val: string) {
      this._ticker = val;
      this.ticker$.next(val ? val.trim().toUpperCase() : '');
   }
   get ticker(): string {
      return this._ticker;
   }

   @Input()
   set active(val: boolean) {
      this._active = val;
      this.active$.next(!!val);
   }
   get active(): boolean {
      return this._active;
   }

   private readonly flowState$: Observable<InternalFlowState>;
   readonly state$: Observable<InsightsTabState>;

   constructor() {
      // Trigger API fetch only when active is true and ticker is non-empty.
      // Do not re-fetch if active changes back and forth without ticker change.
      const initialOrTickerChange$ = combineLatest([
         this.ticker$.pipe(distinctUntilChanged()),
         this.active$,
      ]).pipe(
         filter(([ticker, active]) => !!ticker && active),
         map(([ticker]) => ticker),
         distinctUntilChanged(),
      );

      const retryTrigger$ = this.retry$.pipe(
         switchMap(() =>
            combineLatest([this.ticker$, this.active$]).pipe(
               filter(([ticker, active]) => !!ticker && active),
               map(([ticker]) => ticker),
               takeWhile((_, idx) => idx === 0),
            ),
         ),
      );

      const loadTrigger$ = merge(initialOrTickerChange$, retryTrigger$);

      this.flowState$ = loadTrigger$.pipe(
         switchMap((ticker) => {
            // Reset manual selection when ticker changes or retries
            this.selectedAccessionOverride$.next(null);
            return this.executeFlow(ticker);
         }),
         startWith<InternalFlowState>({
            status: 'idle',
            insights: [],
            selectedAccession: null,
         }),
         takeUntilDestroyed(this.destroyRef),
         shareReplay({ bufferSize: 1, refCount: true }),
      );

      this.state$ = combineLatest([
         this.flowState$,
         this.selectedAccessionOverride$,
      ]).pipe(
         map(([flow, overrideAccession]): InsightsTabState => {
            const effectiveAccession =
               overrideAccession ?? flow.selectedAccession;
            const selectedInsight =
               flow.insights.find(
                  (i) => i.accessionNumber === effectiveAccession,
               ) ?? pickDefaultInsight(flow.insights);

            return {
               status: flow.status,
               pendingMessage: flow.pendingMessage,
               error: flow.error,
               insights: flow.insights,
               selectedInsight,
            };
         }),
         shareReplay({ bufferSize: 1, refCount: true }),
      );
   }

   private executeFlow(ticker: string): Observable<InternalFlowState> {
      return defer(() => {
         const loadingState: InternalFlowState = {
            status: 'loading',
            insights: [],
            selectedAccession: null,
         };

         return this.insightService.getInsights(ticker).pipe(
            switchMap((result: InsightsResult) => {
               if (result.kind === 'ready') {
                  const sorted = sortInsights(result.insights);
                  const def = pickDefaultInsight(sorted);
                  const readyState: InternalFlowState = {
                     status: 'ready',
                     insights: sorted,
                     selectedAccession: def?.accessionNumber ?? null,
                  };
                  return of(readyState);
               }

               return this.pollUntilReady(ticker, result.message);
            }),
            startWith(loadingState),
            catchError((err: unknown) => {
               if (isNotFoundError(err)) {
                  return of<InternalFlowState>({
                     status: 'unavailable',
                     insights: [],
                     selectedAccession: null,
                  });
               }
               return of<InternalFlowState>({
                  status: 'error',
                  error: toApiError(
                     err,
                     'Impossible de charger les synthèses IA.',
                  ),
                  insights: [],
                  selectedAccession: null,
               });
            }),
         );
      });
   }

   private pollUntilReady(
      ticker: string,
      initialMessage: string,
   ): Observable<InternalFlowState> {
      const pendingState: InternalFlowState = {
         status: 'pending',
         pendingMessage: initialMessage,
         insights: [],
         selectedAccession: null,
      };

      let currentInsights: Insight[] = [];
      let currentAccession: string | null = null;

      const timeoutTrigger$ = timer(POLLING_TIMEOUT_MS);

      const pollStream$ = timer(POLLING_INTERVAL_MS, POLLING_INTERVAL_MS).pipe(
         takeUntil(timeoutTrigger$),
         switchMap(() =>
            this.insightService.getStatus(ticker).pipe(
               catchError((err: unknown) =>
                  of({
                     isStatusError: true,
                     error: toApiError(
                        err,
                        'Erreur lors de la vérification du statut.',
                     ),
                  }),
               ),
            ),
         ),
         switchMap((statusRes) => {
            if ('isStatusError' in statusRes) {
               return of<InternalFlowState>({
                  status: 'error',
                  error: statusRes.error,
                  insights: currentInsights,
                  selectedAccession: currentAccession,
               });
            }

            const status = statusRes as IngestionStatus;

            if (!status.ready) {
               return of<InternalFlowState>({
                  status: 'pending',
                  pendingMessage: initialMessage,
                  insights: [],
                  selectedAccession: null,
               });
            }

            // ready is true
            return this.insightService.getInsights(ticker).pipe(
               map((insightsRes: InsightsResult): InternalFlowState => {
                  if (insightsRes.kind === 'ready') {
                     const sorted = sortInsights(insightsRes.insights);
                     currentInsights = sorted;
                     if (!status.inProgress) {
                        currentAccession =
                           pickDefaultInsight(sorted)?.accessionNumber ?? null;
                     } else if (
                        !currentAccession ||
                        !sorted.some(
                           (i) => i.accessionNumber === currentAccession,
                        )
                     ) {
                        currentAccession =
                           pickDefaultInsight(sorted)?.accessionNumber ?? null;
                     }

                     if (status.inProgress) {
                        return {
                           status: 'partial',
                           insights: sorted,
                           selectedAccession: currentAccession,
                        };
                     } else {
                        return {
                           status: 'ready',
                           insights: sorted,
                           selectedAccession: currentAccession,
                        };
                     }
                  }

                  return {
                     status: 'pending',
                     pendingMessage: insightsRes.message,
                     insights: [],
                     selectedAccession: null,
                  };
               }),
               catchError((err: unknown) =>
                  of<InternalFlowState>({
                     status: 'error',
                     error: toApiError(
                        err,
                        'Erreur lors de la récupération des synthèses.',
                     ),
                     insights: currentInsights,
                     selectedAccession: currentAccession,
                  }),
               ),
            );
         }),
         takeWhile((s) => s.status !== 'ready' && s.status !== 'error', true),
      );

      const timeoutState$: Observable<InternalFlowState> = timeoutTrigger$.pipe(
         map((): InternalFlowState => ({
            status: 'timeout',
            insights: [],
            selectedAccession: null,
         })),
      );

      return merge(of(pendingState), pollStream$, timeoutState$).pipe(
         takeWhile(
            (s) =>
               s.status !== 'ready' &&
               s.status !== 'timeout' &&
               s.status !== 'error',
            true,
         ),
      );
   }

   retry(): void {
      this.retry$.next();
   }

   selectReport(accessionNumber: string): void {
      this.selectedAccessionOverride$.next(accessionNumber);
   }

   onReportChange(event: Event): void {
      const select = event.target as HTMLSelectElement;
      this.selectReport(select.value);
   }

   exportPdf(): void {
      window.print();
   }

   isDemo(insight: Insight | null | undefined): boolean {
      return isDemoInsight(insight);
   }

   cleanDemo(text: string | null | undefined): string {
      return cleanDemoPrefix(text);
   }

   hasText(text: string | null | undefined): boolean {
      return !!text && text.trim().length > 0;
   }

   hasList(list: string[] | null | undefined): boolean {
      return !!list && list.length > 0;
   }
}
