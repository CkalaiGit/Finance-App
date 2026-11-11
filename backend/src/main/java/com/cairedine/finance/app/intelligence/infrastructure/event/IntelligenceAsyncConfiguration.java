package com.cairedine.finance.app.intelligence.infrastructure.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
@EnableAsync
class IntelligenceAsyncConfiguration {

    @Bean(name = "intelligenceTaskExecutor", destroyMethod = "close")
    ExecutorService intelligenceTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
