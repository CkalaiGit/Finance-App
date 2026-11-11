import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import {
   InsightsTabComponent,
   POLLING_INTERVAL_MS,
   POLLING_TIMEOUT_MS,
   sortInsights,
   pickDefaultInsight,
} from './insights-tab.component';
import { InsightService } from '../../../../services/insight.service';
import {
   IngestionStatus,
   Insight,
   InsightsResult,
} from '../../../../models/insight.model';
import { ApiError } from '../../../../models/api-error.model';

describe('InsightsTabComponent', () => {
   let component: InsightsTabComponent;
   let fixture: ComponentFixture<InsightsTabComponent>;

   let insightServiceMock: {
      getInsights: ReturnType<typeof vi.fn>;
      getStatus: ReturnType<typeof vi.fn>;
   };

   const sample10K2024: Insight = {
      ticker: 'AAPL',
      formType: '10-K',
      period: 'FY 2024',
      accessionNumber: '0000320193-24-000106',
      synthesePerformance: 'Année record portée par la division Services.',
      analyseMargesEtDette: 'Marge opérationnelle supérieure à 30%.',
      risquesPrincipaux: ['Concurrence IA accrue', 'Risques géopolitiques en Asie'],
      guidanceManagement: 'Croissance à deux chiffres attendue sur les abonnements.',
      chaineApprovisionnement: 'Approvisionnement diversifié en Inde et au Vietnam.',
      faitsMarquants: ['Revenus Services record', 'Rachat d actions massif'],
      generatedAt: '2024-11-01T10:00:00Z',
   };

   const sample10K2023: Insight = {
      ticker: 'AAPL',
      formType: '10-K',
      period: 'FY 2023',
      accessionNumber: '0000320193-23-000106',
      synthesePerformance: 'Performance résiliente dans un contexte macroéconomique incertain.',
      analyseMargesEtDette: 'Excellente gestion des flux de trésorerie disponible.',
      risquesPrincipaux: ['Taux d intérêt élevés'],
      guidanceManagement: 'Prudence sur les dépenses d investissement.',
      chaineApprovisionnement: 'Dépendance soutenue aux usines chinoises.',
      faitsMarquants: ['Progression de l écosystème'],
      generatedAt: '2023-11-01T10:00:00Z',
   };

   const sample10Q2024Q3: Insight = {
      ticker: 'AAPL',
      formType: '10-Q',
      period: '2024-Q3',
      accessionNumber: '0000320193-24-000088',
      synthesePerformance: 'Troisième trimestre dynamique.',
      analyseMargesEtDette: 'Marge brute stable.',
      risquesPrincipaux: ['Fluctuations de change'],
      guidanceManagement: null,
      chaineApprovisionnement: null,
      faitsMarquants: ['Croissance de l iPad'],
      generatedAt: '2024-08-01T10:00:00Z',
   };

   const sample10Q2024Q1: Insight = {
      ticker: 'AAPL',
      formType: '10-Q',
      period: '2024-Q1',
      accessionNumber: '0000320193-24-000006',
      synthesePerformance: 'Premier trimestre conforme aux attentes.',
      analyseMargesEtDette: 'Dette nette sous contrôle.',
      risquesPrincipaux: ['Tensions logistiques'],
      guidanceManagement: null,
      chaineApprovisionnement: null,
      faitsMarquants: ['Fêtes de fin d année réussies'],
      generatedAt: '2024-02-01T10:00:00Z',
   };

   beforeEach(async () => {
      insightServiceMock = {
         getInsights: vi.fn().mockReturnValue(
            of<InsightsResult>({
               kind: 'ready',
               insights: [sample10K2024],
            }),
         ),
         getStatus: vi.fn().mockReturnValue(
            of<IngestionStatus>({
               ticker: 'AAPL',
               ready: true,
               inProgress: false,
               count: 1,
               lastGenerated: '2024-11-01T10:00:00Z',
            }),
         ),
      };

      await TestBed.configureTestingModule({
         imports: [InsightsTabComponent],
         providers: [
            { provide: InsightService, useValue: insightServiceMock },
         ],
      }).compileComponents();

      fixture = TestBed.createComponent(InsightsTabComponent);
      component = fixture.componentInstance;
   });

   afterEach(() => {
      vi.useRealTimers();
   });

   it('aucun appel avant l ouverture de l onglet', () => {
      component.ticker = 'AAPL';
      component.active = false;
      fixture.detectChanges();

      expect(insightServiceMock.getInsights).not.toHaveBeenCalled();
      expect(insightServiceMock.getStatus).not.toHaveBeenCalled();

      // Quand l'onglet devient actif, l'appel API est déclenché
      component.active = true;
      fixture.detectChanges();

      expect(insightServiceMock.getInsights).toHaveBeenCalledWith('AAPL');
   });

   it('pending puis ready après sondage', () => {
      vi.useFakeTimers();

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'pending',
            message: 'Ingestion des rapports SEC en cours...',
         }),
      );

      // Statut 1 : encore en cours, pas prêt
      insightServiceMock.getStatus.mockReturnValueOnce(
         of<IngestionStatus>({
            ticker: 'AAPL',
            ready: false,
            inProgress: true,
            count: 0,
            lastGenerated: null,
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      // État pending initial
      let compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('[data-testid="insights-pending"]')).toBeTruthy();
      expect(compiled.textContent).toContain('Ingestion des rapports SEC en cours...');

      // Avancer de 5 secondes pour le premier tick de polling
      vi.advanceTimersByTime(POLLING_INTERVAL_MS);

      // Statut 2 : prêt et inProgress false
      insightServiceMock.getStatus.mockReturnValueOnce(
         of<IngestionStatus>({
            ticker: 'AAPL',
            ready: true,
            inProgress: false,
            count: 1,
            lastGenerated: '2024-11-01T10:00:00Z',
         }),
      );

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'ready',
            insights: [sample10K2024],
         }),
      );

      // Deuxième tick de polling
      vi.advanceTimersByTime(POLLING_INTERVAL_MS);
      fixture.detectChanges();

      compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('[data-testid="insights-ready"]')).toBeTruthy();
      expect(compiled.textContent).toContain('Année record portée par la division Services.');
   });

   it('état partiel puis rechargement', () => {
      vi.useFakeTimers();

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'pending',
            message: 'Ingestion en cours...',
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      // 1er sondage : ready est vrai ET inProgress est vrai (état partiel)
      insightServiceMock.getStatus.mockReturnValueOnce(
         of<IngestionStatus>({
            ticker: 'AAPL',
            ready: true,
            inProgress: true,
            count: 1,
            lastGenerated: null,
         }),
      );

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'ready',
            insights: [sample10Q2024Q3],
         }),
      );

      vi.advanceTimersByTime(POLLING_INTERVAL_MS);
      fixture.detectChanges();

      let compiled = fixture.nativeElement as HTMLElement;
      const partialEl = compiled.querySelector('[data-testid="insights-partial"]');
      expect(partialEl).toBeTruthy();
      expect(compiled.textContent).toContain('analyse des autres rapports en cours');
      expect(compiled.textContent).toContain('Troisième trimestre dynamique.');

      // 2e sondage : inProgress devient faux -> rechargement de la liste
      insightServiceMock.getStatus.mockReturnValueOnce(
         of<IngestionStatus>({
            ticker: 'AAPL',
            ready: true,
            inProgress: false,
            count: 2,
            lastGenerated: '2024-11-01T10:00:00Z',
         }),
      );

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'ready',
            insights: [sample10K2024, sample10Q2024Q3],
         }),
      );

      vi.advanceTimersByTime(POLLING_INTERVAL_MS);
      fixture.detectChanges();

      compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('[data-testid="insights-ready"]')).toBeTruthy();
      expect(compiled.querySelector('[data-testid="insights-partial"]')).toBeFalsy();
      expect(compiled.textContent).toContain('Année record portée par la division Services.');
   });

   it('timeout à 3 minutes', () => {
      vi.useFakeTimers();

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'pending',
            message: 'Traitement long...',
         }),
      );

      insightServiceMock.getStatus.mockReturnValue(
         of<IngestionStatus>({
            ticker: 'AAPL',
            ready: false,
            inProgress: true,
            count: 0,
            lastGenerated: null,
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      // Avancer jusqu'au timeout de 3 minutes
      vi.advanceTimersByTime(POLLING_TIMEOUT_MS);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const timeoutEl = compiled.querySelector('[data-testid="insights-timeout"]');
      expect(timeoutEl).toBeTruthy();
      expect(compiled.textContent).toContain("Délai d'attente dépassé");

      // Bouton Réessayer
      const retryBtn = timeoutEl?.querySelector('button') as HTMLButtonElement;
      expect(retryBtn).toBeTruthy();
      expect(retryBtn.textContent).toContain('Réessayer');

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'ready',
            insights: [sample10K2024],
         }),
      );

      retryBtn.click();
      fixture.detectChanges();

      expect(insightServiceMock.getInsights).toHaveBeenCalledTimes(2);
   });

   it('404 société non cotée aux États-Unis', () => {
      const notFoundError: ApiError = {
         status: 404,
         title: 'Non trouvé',
         detail: 'Ticker inconnu de la SEC',
      };
      insightServiceMock.getInsights.mockReturnValue(throwError(() => notFoundError));

      component.ticker = 'UNKNOWN';
      component.active = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const unavailableEl = compiled.querySelector('[data-testid="insights-unavailable"]');
      expect(unavailableEl).toBeTruthy();
      expect(compiled.textContent).toContain('Aucun rapport SEC pour cette entreprise');
   });

   it('erreur et bouton Réessayer', () => {
      const serverError: ApiError = {
         status: 500,
         title: 'Erreur serveur SEC',
         detail: 'Impossible de joindre le service SEC EDGAR',
      };
      insightServiceMock.getInsights.mockReturnValueOnce(throwError(() => serverError));

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      let compiled = fixture.nativeElement as HTMLElement;
      const errorEl = compiled.querySelector('[data-testid="insights-error"]');
      expect(errorEl).toBeTruthy();
      expect(compiled.textContent).toContain('Erreur serveur SEC');
      expect(compiled.textContent).toContain('Impossible de joindre le service SEC EDGAR');

      const retryBtn = errorEl?.querySelector('button') as HTMLButtonElement;
      expect(retryBtn).toBeTruthy();
      expect(retryBtn.textContent).toContain('Réessayer');

      insightServiceMock.getInsights.mockReturnValueOnce(
         of<InsightsResult>({
            kind: 'ready',
            insights: [sample10K2024],
         }),
      );

      retryBtn.click();
      fixture.detectChanges();

      expect(insightServiceMock.getInsights).toHaveBeenCalledTimes(2);
      compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('[data-testid="insights-ready"]')).toBeTruthy();
   });

   it('arrêt du sondage à la destruction', () => {
      vi.useFakeTimers();

      insightServiceMock.getInsights.mockReturnValue(
         of<InsightsResult>({
            kind: 'pending',
            message: 'En attente...',
         }),
      );

      insightServiceMock.getStatus.mockReturnValue(
         of<IngestionStatus>({
            ticker: 'AAPL',
            ready: false,
            inProgress: true,
            count: 0,
            lastGenerated: null,
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      // Tick 1
      vi.advanceTimersByTime(POLLING_INTERVAL_MS);
      expect(insightServiceMock.getStatus).toHaveBeenCalledTimes(1);

      // Détruire le composant
      fixture.destroy();

      // Avancer davantage dans le temps
      vi.advanceTimersByTime(POLLING_INTERVAL_MS * 5);
      expect(insightServiceMock.getStatus).toHaveBeenCalledTimes(1);
   });

   it('blocs null masqués', () => {
      const insightWithNulls: Insight = {
         ...sample10K2024,
         guidanceManagement: null,
         chaineApprovisionnement: null,
      };

      insightServiceMock.getInsights.mockReturnValue(
         of<InsightsResult>({
            kind: 'ready',
            insights: [insightWithNulls],
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      // Blocs présents
      expect(compiled.textContent).toContain('Faits marquants');
      expect(compiled.textContent).toContain('Synthèse de la performance');
      expect(compiled.textContent).toContain('Marges et dette');
      expect(compiled.textContent).toContain('Risques principaux');

      // Blocs null masqués
      expect(compiled.textContent).not.toContain('Guidance du management');
      expect(compiled.textContent).not.toContain('Chaîne d approvisionnement');
   });

   it('badge de démonstration et préfixe retiré', () => {
      const demoInsight: Insight = {
         ...sample10K2024,
         synthesePerformance: '[DÉMO] Performance synthétisée par IA de test',
         analyseMargesEtDette: '[DÉMO] Marges brutes en amélioration continue',
         guidanceManagement: '[DÉMO] Objectifs annuels confirmés',
         chaineApprovisionnement: '[DÉMO] Aucun goulet d étranglement détecté',
         faitsMarquants: ['[DÉMO] Nouveau contrat majeur', '[DÉMO] Rachat d actions'],
         risquesPrincipaux: ['[DÉMO] Risque de change'],
      };

      insightServiceMock.getInsights.mockReturnValue(
         of<InsightsResult>({
            kind: 'ready',
            insights: [demoInsight],
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const demoBadge = compiled.querySelector('[data-testid="insights-demo"]');
      expect(demoBadge).toBeTruthy();
      expect(demoBadge?.textContent).toContain('Données de démonstration');

      // Le texte affiché ne doit plus contenir le préfixe [DÉMO]
      expect(compiled.textContent).not.toContain('[DÉMO]');
      expect(compiled.textContent).toContain('Performance synthétisée par IA de test');
      expect(compiled.textContent).toContain('Marges brutes en amélioration continue');
      expect(compiled.textContent).toContain('Nouveau contrat majeur');
   });

   it('tri et sélection par défaut', () => {
      const unsorted = [
         sample10Q2024Q3,
         sample10K2023,
         sample10K2024,
         sample10Q2024Q1,
      ];

      const sorted = sortInsights(unsorted);
      // 10-K d'abord (du plus récent au plus ancien), puis 10-Q (du plus récent au plus ancien)
      expect(sorted[0].accessionNumber).toBe(sample10K2024.accessionNumber);
      expect(sorted[1].accessionNumber).toBe(sample10K2023.accessionNumber);
      expect(sorted[2].accessionNumber).toBe(sample10Q2024Q3.accessionNumber);
      expect(sorted[3].accessionNumber).toBe(sample10Q2024Q1.accessionNumber);

      // Le 10-K le plus récent est sélectionné par défaut
      const defaultPick = pickDefaultInsight(sorted);
      expect(defaultPick?.period).toBe('FY 2024');

      // Si aucun 10-K, sélectionne le premier élément
      const onlyQuarterly = [sample10Q2024Q3, sample10Q2024Q1];
      expect(pickDefaultInsight(onlyQuarterly)?.period).toBe('2024-Q3');

      insightServiceMock.getInsights.mockReturnValue(
         of<InsightsResult>({
            kind: 'ready',
            insights: unsorted,
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const selectEl = compiled.querySelector('.report-select') as HTMLSelectElement;
      expect(selectEl).toBeTruthy();

      const options = selectEl.querySelectorAll('option');
      expect(options.length).toBe(4);
      expect(options[0].textContent?.trim()).toBe('10-K · FY 2024');
      expect(options[1].textContent?.trim()).toBe('10-K · FY 2023');
      expect(options[2].textContent?.trim()).toBe('10-Q · 2024-Q3');
      expect(options[3].textContent?.trim()).toBe('10-Q · 2024-Q1');

      // Le 10-K le plus récent (FY 2024) est affiché
      expect(compiled.textContent).toContain(sample10K2024.synthesePerformance);
   });

   it('bouton Exporter en PDF visible en état ready et appelle window.print()', () => {
      const printSpy = vi.spyOn(window, 'print').mockImplementation(() => {});

      insightServiceMock.getInsights.mockReturnValue(
         of<InsightsResult>({
            kind: 'ready',
            insights: [sample10K2024],
         }),
      );

      component.ticker = 'AAPL';
      component.active = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const printBtn = compiled.querySelector('[data-testid="export-pdf-btn"]') as HTMLButtonElement;
      expect(printBtn).toBeTruthy();

      printBtn.click();
      expect(printSpy).toHaveBeenCalledTimes(1);

      printSpy.mockRestore();
   });
});
