package com.cairedine.finance.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FinanceappApplicationTests {

	@Test
	void contextLoads() {
	}

    private final ApplicationModules modules =
            ApplicationModules.of(FinanceApp.class);

    @Test
    void verifyStructureDesModules() {
        modules.verify();
    }

    @Test
    void generateModulesDoc() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

}
