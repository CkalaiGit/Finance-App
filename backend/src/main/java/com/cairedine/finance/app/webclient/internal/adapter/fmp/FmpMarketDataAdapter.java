package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import com.cairedine.finance.app.webclient.*;
import com.cairedine.finance.app.webclient.internal.dto.*;
import com.cairedine.finance.app.webclient.internal.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class FmpMarketDataAdapter implements IMarketDataPort {

    private final RestClient fmpRestClient;
    private final CompanyProfileMapper companyProfileMapper;
    private final IncomeStatementMapper incomeStatementMapper;
    private final CashFlowMapper cashFlowMapper;
    private final AnalystEstimateMapper analystEstimateMapper;
    private final KeyMetricsMapper keyMetricsMapper;

    @Override
    public Optional<CompanyProfileRecord> fetchCompanyProfile(String symbol) {
        FmpCompanyProfileDto[] dtos = fmpRestClient.get()
                .uri("/stable/profile?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpCompanyProfileDto[].class);

        if (dtos == null || dtos.length == 0) return Optional.empty();

        return Optional.ofNullable(companyProfileMapper.toRecord(dtos[0]));
    }

    @Override
    public List<IncomeStatementRecord> fetchIncomeStatements(String symbol, int limit) {
        FmpIncomeStatementTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/income-statement?symbol={symbol}&limit={limit}", symbol, limit)
                .retrieve()
                .body(FmpIncomeStatementTtmDto[].class);

        if (dtos == null || dtos.length == 0) return List.of();

        return Arrays.stream(dtos)
                .map(incomeStatementMapper::toRecord)
                .toList();
    }

    @Override
    public Optional<CashFlowRecord> fetchCashFlowTtm(String symbol) {
        FmpCashFlowTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/cash-flow-statement?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpCashFlowTtmDto[].class);

        if (dtos == null || dtos.length == 0) return Optional.empty();

        return Optional.ofNullable(cashFlowMapper.toRecord(dtos[0]));
    }

    @Override
    public List<CashFlowRecord> fetchCashFlowStatements(String symbol, int limit) {
        FmpCashFlowTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/cash-flow-statement?symbol={symbol}&limit={limit}", symbol, limit)
                .retrieve()
                .body(FmpCashFlowTtmDto[].class);

        if (dtos == null || dtos.length == 0) return List.of();

        return Arrays.stream(dtos)
                .map(cashFlowMapper::toRecord)
                .toList();
    }

    @Override
    public Optional<KeyMetricsRecord> fetchKeyMetricsTtm(String symbol) {
        // Fetch from key-metrics-ttm
        FmpKeyMetricsTtmDto[] kmDtos = fmpRestClient.get()
                .uri("/stable/key-metrics-ttm?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpKeyMetricsTtmDto[].class);

        // Fetch from ratios-ttm (for PE ratio)
        FmpKeyMetricsTtmDto[] ratioDtos = fmpRestClient.get()
                .uri("/stable/ratios-ttm?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpKeyMetricsTtmDto[].class);

        if (kmDtos == null || kmDtos.length == 0) return Optional.empty();

        FmpKeyMetricsTtmDto km = kmDtos[0];
        Double pe = (ratioDtos != null && ratioDtos.length > 0) ? ratioDtos[0].peRatioTTM() : null;

        FmpKeyMetricsTtmDto merged = new FmpKeyMetricsTtmDto(
                km.symbol(),
                km.returnOnInvestedCapitalTTM(),
                km.netDebtToEBITDATTM(),
                km.enterpriseValueTTM(),
                pe,
                km.evToSalesTTM()
        );

        return Optional.ofNullable(keyMetricsMapper.toRecord(merged));
    }

    @Override
    public List<AnalystEstimateRecord> fetchAnalystEstimates(String symbol) {
        FmpAnalystEstimateDto[] dtos = fmpRestClient.get()
                .uri("/stable/analyst-estimates?symbol={symbol}&period=annual&limit=2", symbol)
                .retrieve()
                .body(FmpAnalystEstimateDto[].class);

        if (dtos == null || dtos.length == 0) return List.of();

        return Arrays.stream(dtos)
                .map(analystEstimateMapper::toRecord)
                .toList();
    }
}
