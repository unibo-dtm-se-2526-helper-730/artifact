package it.helper730;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Architecture rules of the backend, checked on the compiled classes. */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("it.helper730");

    @Test
    void domainAndEngineDoNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAnyPackage("..domain..", "..engine..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "com.fasterxml.jackson..",
                        "tools.jackson..")
                .check(CLASSES);
    }

    @Test
    void domainDependsOnNoOtherLayer() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..engine..",
                        "..ingestion..",
                        "..reporting..",
                        "..persistence..",
                        "..service..",
                        "..api..",
                        "..config..",
                        "..i18n..")
                .check(CLASSES);
    }

    /** The engine is the core: it may use the domain, and no other layer. */
    @Test
    void engineDependsOnlyOnDomain() {
        noClasses()
                .that()
                .resideInAPackage("..engine..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..ingestion..",
                        "..reporting..",
                        "..persistence..",
                        "..service..",
                        "..api..",
                        "..config..",
                        "..i18n..")
                .check(CLASSES);
    }

    /**
     * Each layer depends only on layers below it.
     *
     * <pre>
     * api ──► service ──► ingestion / engine / reporting / persistence ──► domain
     * </pre>
     *
     * The api layer may also use engine and reporting types directly (exceptions
     * and declaration items it maps to DTOs). config wires the framework-free core
     * into Spring, so it may use the layers it builds beans for.
     */
    @Test
    void layersDependOnlyOnLayersBelow() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Api")
                .definedBy("..api..")
                .layer("Service")
                .definedBy("..service..")
                .layer("Ingestion")
                .definedBy("..ingestion..")
                .layer("Engine")
                .definedBy("..engine..")
                .layer("Reporting")
                .definedBy("..reporting..")
                .layer("Persistence")
                .definedBy("..persistence..")
                .layer("Domain")
                .definedBy("..domain..")
                .layer("I18n")
                .definedBy("..i18n..")
                .layer("Config")
                .definedBy("..config..")
                .whereLayer("Api")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Config")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Service")
                .mayOnlyBeAccessedByLayers("Api")
                .whereLayer("Persistence")
                .mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Ingestion")
                .mayOnlyBeAccessedByLayers("Api", "Service", "Config")
                .whereLayer("Engine")
                .mayOnlyBeAccessedByLayers("Api", "Service", "Config")
                .whereLayer("Reporting")
                .mayOnlyBeAccessedByLayers("Api", "Service", "Config")
                .whereLayer("I18n")
                .mayOnlyBeAccessedByLayers("Api", "Config")
                .check(CLASSES);
    }
}
