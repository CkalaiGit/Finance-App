package com.cairedine.finance.app.financialanalysis.infrastructure.web.controller;

import com.cairedine.finance.app.financialanalysis.domain.port.IFinancialAnalysisService;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.dto.FullMetricsResponse;
import com.cairedine.finance.app.financialanalysis.infrastructure.web.mapper.FinancialAnalysisWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class FinancialAnalysisController {

    private final IFinancialAnalysisService financialAnalysisService;
    private final FinancialAnalysisWebMapper webMapper;

    @GetMapping("/{ticker}")
    public ResponseEntity<List<FullMetricsResponse>> getMetrics(@PathVariable String ticker) {
        var domainList = financialAnalysisService.computeMetrics(ticker);
        return ResponseEntity.ok(domainList.stream()
                .map(webMapper::toResponse)
                .toList());
    }
}
