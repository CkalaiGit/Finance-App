package com.cairedine.finance.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
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

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {}

    @Test
    void startsWithStubAndWithoutGeminiApiKey() {
        Assertions.assertThat(applicationContext.getBean(
                com.cairedine.finance.app.intelligence.domain.port.IInsightSynthesizer.class)
                .getClass().getSimpleName()).isEqualTo("StubInsightSynthesizer");
        Assertions.assertThat(environment.getProperty("spring.ai.google.genai.api-key")).isBlank();
        Assertions.assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");
    }

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
    void verifyWatchlistModulePublicAPI() throws Exception {
        Path watchlistRoot = Paths.get("src/main/java/com/cairedine/finance/app/watchlist");
        var classes = Files.list(watchlistRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.lines(p).limit(5)
                                .anyMatch(l -> l.trim().equals("package com.cairedine.finance.app.watchlist;"));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(p -> p.getFileName().toString().replace(".java", ""))
                .filter(name -> !name.equals("package-info"))
                .collect(Collectors.toSet());

        Assertions.assertThat(classes).containsExactly("TickerAddedEvent");
    }

    @Test
    void generateModulesDoc() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

}
