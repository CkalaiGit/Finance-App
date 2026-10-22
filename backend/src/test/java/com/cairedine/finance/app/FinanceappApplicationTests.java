package com.cairedine.finance.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FinanceappApplicationTests {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {}

    private final ApplicationModules modules =
            ApplicationModules.of(FinanceApp.class);

    @Test
    void verifyModulesStructure() {
        modules.verify();
    }

    @Test
    void verifyUserModulePublicAPI() throws Exception {

        Path userRoot = Paths.get("src/main/java/com/cairedine/finance/app/user");
        var classes = Files.list(userRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.lines(p).limit(5)
                                .anyMatch(l -> l.trim().equals("package com.cairedine.finance.app.user;"));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(p -> p.getFileName().toString().replace(".java", ""))
                .filter(name -> !name.equals("package-info"))
                .collect(Collectors.toSet());

        // Seuls ces deux types doivent déclarer le package racine du module "user"
        Assertions.assertThat(classes).containsExactlyInAnyOrder("UserContext", "UserSyncedEvent");
    }

    @Test
    void generateModulesDoc() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

}
