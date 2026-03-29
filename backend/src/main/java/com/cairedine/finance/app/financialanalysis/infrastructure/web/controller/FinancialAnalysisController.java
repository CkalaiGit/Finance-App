package com.cairedine.finance.app.financialanalysis.infrastructure.web.controller;

import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.GrowthMetricsResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper.FinancialAnalysisWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class FinancialAnalysisController {

    private final IFinancialAnalysisService financialAnalysisService;
    private final FinancialAnalysisWebMapper webMapper;

    @GetMapping("/{ticker}")
    public ResponseEntity<GrowthMetricsResponse> getMetrics(@PathVariable String ticker) {
        var domain = financialAnalysisService.computeMetrics(ticker);
        return ResponseEntity.ok(webMapper.toResponse(domain));
    }
}
