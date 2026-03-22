package com.cairedine.finance.app.webclient.internal.adapter.fmp;

import com.cairedine.finance.app.webclient.*;
import com.cairedine.finance.app.webclient.internal.dto.FmpAnalystEstimateDto;
import com.cairedine.finance.app.webclient.internal.dto.FmpCashFlowTtmDto;
import com.cairedine.finance.app.webclient.internal.dto.FmpCompanyProfileDto;
import com.cairedine.finance.app.webclient.internal.dto.FmpIncomeStatementTtmDto;
import com.cairedine.finance.app.webclient.internal.mapper.AnalystEstimateMapper;
import com.cairedine.finance.app.webclient.internal.mapper.CashFlowMapper;
import com.cairedine.finance.app.webclient.internal.mapper.CompanyProfileMapper;
import com.cairedine.finance.app.webclient.internal.mapper.IncomeStatementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class FmpMarketDataAdapter implements IMarketDataPort {

    private final RestClient fmpRestClient;
    private final CompanyProfileMapper companyProfileMapper;
    private final IncomeStatementMapper incomeStatementMapper;
    private final CashFlowMapper cashFlowMapper;
    private final AnalystEstimateMapper analystEstimateMapper;

    @Override
    public CompanyProfileRecord fetchCompanyProfile(String symbol) {
        FmpCompanyProfileDto[] dtos = fmpRestClient.get()
                .uri("/stable/profile?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpCompanyProfileDto[].class);
        assert dtos != null;
        return companyProfileMapper.toRecord(dtos[0]);
    }

    @Override
    public List<IncomeStatementRecord> fetchIncomeStatements(String symbol, int limit) {
        FmpIncomeStatementTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/income-statement?symbol={symbol}&limit={limit}", symbol, limit)
                .retrieve()
                .body(FmpIncomeStatementTtmDto[].class);

        if (dtos == null || dtos.length == 0) {
            return List.of();
        }

        return Arrays.stream(dtos)
                .map(incomeStatementMapper::toRecord)
                .toList();
    }

    @Override
    public CashFlowRecord fetchCashFlowTtm(String symbol) {
        FmpCashFlowTtmDto[] dtos = fmpRestClient.get()
                .uri("/stable/cash-flow-statement?symbol={symbol}", symbol)
                .retrieve()
                .body(FmpCashFlowTtmDto[].class);
        assert dtos != null;
        return cashFlowMapper.toRecord(dtos[0]);
    }

    @Override
    public List<AnalystEstimateRecord> fetchAnalystEstimates(String symbol) {
        FmpAnalystEstimateDto[] dtos = fmpRestClient.get()
                .uri("/stable/analyst-estimates?symbol={symbol}&period=annual&limit=2", symbol)
                .retrieve()
                .body(FmpAnalystEstimateDto[].class);

        assert dtos != null;
        return Arrays.stream(dtos)
                .map(analystEstimateMapper::toRecord)
                .toList();
    }
}
