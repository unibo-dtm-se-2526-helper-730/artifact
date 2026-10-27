package it.helper730;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
                        "..config..")
                .check(CLASSES);
    }
}
