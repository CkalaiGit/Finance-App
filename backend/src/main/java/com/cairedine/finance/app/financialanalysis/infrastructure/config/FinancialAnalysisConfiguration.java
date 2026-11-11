package com.cairedine.finance.app.financialanalysis.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class FinancialAnalysisConfiguration {

    @Bean
    Clock financialAnalysisClock() {
        return Clock.systemUTC();
    }
}
