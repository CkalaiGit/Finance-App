export interface Insight {
   ticker: string;
   formType: '10-K' | '10-Q';
   period: string;
   accessionNumber: string;
   synthesePerformance: string;
   analyseMargesEtDette: string;
   risquesPrincipaux: string[];
   guidanceManagement: string | null;
   chaineApprovisionnement: string | null;
   faitsMarquants: string[];
   generatedAt: string;
}

export interface IngestionStatus {
   ticker: string;
   ready: boolean;
   inProgress: boolean;
   count: number;
   lastGenerated: string | null;
}

export type InsightsResult =
   | { kind: 'ready'; insights: Insight[] }
   | { kind: 'pending'; message: string };
