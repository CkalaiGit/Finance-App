import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
   IngestionStatus,
   Insight,
   InsightsResult,
} from '../models/insight.model';
import { environment } from '../../environments/environment';

interface PendingBody {
   status?: string;
   message?: string;
}

@Injectable({
   providedIn: 'root',
})
export class InsightService {
   private readonly http = inject(HttpClient);
   private readonly baseUrl = `${environment.apiUrl}/insights`;

   getInsights(ticker: string): Observable<InsightsResult> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http
         .get<Insight[] | PendingBody>(`${this.baseUrl}/${cleanTicker}`, {
            observe: 'response',
         })
         .pipe(
            map((response: HttpResponse<Insight[] | PendingBody>): InsightsResult => {
               if (response.status === 202) {
                  const pendingBody = response.body as PendingBody | null;
                  return {
                     kind: 'pending',
                     message:
                        pendingBody?.message ||
                        'Ingestion des rapports SEC en cours...',
                  };
               }

               return {
                  kind: 'ready',
                  insights: (response.body as Insight[]) ?? [],
               };
            }),
         );
   }

   getStatus(ticker: string): Observable<IngestionStatus> {
      const cleanTicker = ticker.trim().toUpperCase();
      return this.http.get<IngestionStatus>(
         `${this.baseUrl}/${cleanTicker}/status`,
      );
   }
}
