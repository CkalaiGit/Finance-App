import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { FullMetrics } from '../models/analysis.model';
import { environment } from '../../environments/environment';

@Injectable({
   providedIn: 'root',
})
export class FinancialAnalysisService {
   private readonly http = inject(HttpClient);
   private readonly baseUrl = `${environment.apiUrl}/analysis`;

   getMetrics(ticker: string): Observable<FullMetrics | null> {
      return this.http
         .get<FullMetrics[]>(`${this.baseUrl}/${ticker.toUpperCase()}`)
         .pipe(
            map((metricsList) =>
               metricsList && metricsList.length > 0
                  ? metricsList[0]
                  : null,
            ),
         );
   }
}
