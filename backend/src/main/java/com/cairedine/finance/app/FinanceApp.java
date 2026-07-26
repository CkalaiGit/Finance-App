package com.cairedine.finance.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class FinanceApp {

	static void main(String[] args) {
		SpringApplication.run(FinanceApp.class, args);
	}

}

