package com.cupflow.pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.shared.security.AuthenticatedEndpoint;
import com.cupflow.pos.shared.security.PublicEndpoint;
import com.cupflow.pos.shared.security.RequiresRole;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {

    private final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.cupflow.pos");

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

    @Test
    void everyControllerEndpointDeclaresItsAccessRule() {
        List<String> undeclaredEndpoints = productionClasses.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .filter(this::isRequestHandler)
                .filter(method -> !hasAccessDeclaration(method))
                .map(JavaMethod::getFullName)
                .sorted()
                .toList();

        assertThat(undeclaredEndpoints)
                .as("Every production controller endpoint must be public, authenticated, or role protected")
                .isEmpty();
    }

    private boolean isRequestHandler(JavaMethod method) {
        return method.isAnnotatedWith(GetMapping.class)
                || method.isAnnotatedWith(PostMapping.class)
                || method.isAnnotatedWith(PutMapping.class)
                || method.isAnnotatedWith(PatchMapping.class)
                || method.isAnnotatedWith(DeleteMapping.class)
                || method.isAnnotatedWith(RequestMapping.class);
    }

    private boolean hasAccessDeclaration(JavaMethod method) {
        return method.isAnnotatedWith(PublicEndpoint.class)
                || method.isAnnotatedWith(AuthenticatedEndpoint.class)
                || method.isAnnotatedWith(RequiresRole.class)
                || method.getOwner().isAnnotatedWith(PublicEndpoint.class)
                || method.getOwner().isAnnotatedWith(AuthenticatedEndpoint.class)
                || method.getOwner().isAnnotatedWith(RequiresRole.class);
    }
}
