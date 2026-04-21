import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { FullMetrics } from '../models/analysis.model';

@Injectable({
  providedIn: 'root'
})
export class FinancialAnalysisService {
  private readonly baseUrl = 'http://localhost:8081/api/v1/analysis';

  constructor(private readonly http: HttpClient) {}

  getMetrics(ticker: string): Observable<FullMetrics> {
    return this.http.get<FullMetrics[]>(`${this.baseUrl}/${ticker.toUpperCase()}`).pipe(
      map(metricsList => metricsList && metricsList.length > 0 ? metricsList[0] : null as any)
    );
  }
}
