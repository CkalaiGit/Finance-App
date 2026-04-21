export interface GrowthMetrics {
    revenueGrowth3Y: number | null;
    ebitdaGrowth3Y: number | null;
    epsGrowth3Y: number | null;
    fcfGrowth: number | null;
}

export interface ValueMetrics {
    evToEbit: number | null;
    peRatioTTM: number | null;
    pegRatioForward: number | null;
    evToSales: number | null;
}

export interface QualityMetrics {
    roic: number | null;
    operatingMargin: number | null;
    netDebtToEbitda: number | null;
    freeCashFlowMargin: number | null;
    sgaToRevenue: number | null;
}

export interface FullMetrics {
    growth: GrowthMetrics;
    value: ValueMetrics;
    quality: QualityMetrics;
    fiscalYearEndDate: string | null;
    marketDataAsOf: string | null;
}
