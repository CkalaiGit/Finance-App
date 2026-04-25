import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Observable, of } from 'rxjs';
import { switchMap, catchError, map, distinctUntilChanged } from 'rxjs/operators';
import { FinancialAnalysisService } from '../../../services/financial-analysis.service';
import { CompanyService } from '../../../services/company.service';
import { FullMetrics } from '../../../models/analysis.model';
import { CompanyDisplayDTO } from '../../../models/company.model';

@Component({
  selector: 'app-company-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './company-details.component.html',
  styleUrls: ['./company-details.component.scss']
})
export class CompanyDetailsComponent implements OnInit {
  activeTab: 'growth' | 'value' | 'quality' = 'growth';

  metrics$!: Observable<FullMetrics | null>;
  company$!: Observable<CompanyDisplayDTO | null>;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly analysisService: FinancialAnalysisService,
    private readonly companyService: CompanyService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.pipe(
      map(params => params.get('ticker') || ''),
      distinctUntilChanged(),
      switchMap(ticker => {
        if (!ticker) return of(null);
        this.company$ = this.companyService.getByTicker(ticker).pipe(catchError(() => of(null)));
        this.metrics$ = this.analysisService.getMetrics(ticker).pipe(catchError(() => of(null)));
        return of(ticker);
      })
    ).subscribe();
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
