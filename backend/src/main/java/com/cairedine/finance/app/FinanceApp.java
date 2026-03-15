package com.cairedine.finance.app;

import com.cairedine.finance.app.webclient.internal.adapter.fmp.FmpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FmpProperties.class)
public class FinanceApp {

	static void main(String[] args) {
		SpringApplication.run(FinanceApp.class, args);
	}

}

