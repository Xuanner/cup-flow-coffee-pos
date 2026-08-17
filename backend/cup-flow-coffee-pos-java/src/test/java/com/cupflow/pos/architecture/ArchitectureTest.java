package com.cupflow.pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private final JavaClasses productionClasses = new ClassFileImporter().importPackages("com.cupflow.pos");

    @Test
    void businessModulesAreFreeOfCycles() {
        slices().matching("com.cupflow.pos.(*)..").should().beFreeOfCycles().check(productionClasses);
    }

    @Test
    void domainCodeDoesNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "org.mybatis..", "jakarta.persistence..")
                .check(productionClasses);
    }
}
