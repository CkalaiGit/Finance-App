package com.cairedine.finance.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Règles d'architecture ArchUnit vérifiant les conventions de packages.
 * Convention cible :
 *   &lt;module&gt;/domain/       → modèles, ports, règles métier. Java pur (aucun framework).
 *   &lt;module&gt;/application/  → services de cas d'usage. @Service autorisé ici.
 *   &lt;module&gt;/infrastructure/ → adapters web/, persistence/, security/, event/...
 */
@DisplayName("Architecture Rules")
class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "com.cairedine.finance.app";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Règle 1 : Le domaine ne dépend pas de frameworks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Domain purity rules")
    class DomainPurityRules {

        @Test
        @DisplayName("Le domaine ne dépend pas de org.springframework")
        void domainShouldNotDependOnSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Le domaine ne dépend pas de jakarta.persistence")
        void domainShouldNotDependOnJakartaPersistence() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("jakarta.persistence..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Le domaine ne dépend pas de com.fasterxml (Jackson)")
        void domainShouldNotDependOnJackson() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.fasterxml..");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("Le domaine ne dépend pas de l'infrastructure")
        void domainShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");

            rule.check(importedClasses);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Règle 2 : Les @RestController résident dans infrastructure.web
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Controller placement rules")
    class ControllerPlacementRules {

        @Test
        @DisplayName("Les @RestController résident dans ..infrastructure.web..")
        void restControllersShouldResideInInfrastructureWeb() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().resideInAPackage("..infrastructure.web..");

            rule.check(importedClasses);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Règle 3 : Les @Service résident dans application ou infrastructure
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Service placement rules")
    class ServicePlacementRules {

        @Test
        @DisplayName("Les @Service résident dans ..application.. ou ..infrastructure..")
        void servicesShouldResideInApplicationOrInfrastructure() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().resideInAnyPackage("..application..", "..infrastructure..");

            rule.check(importedClasses);
        }
    }
}
