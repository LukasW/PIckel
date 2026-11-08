package ch.grudligstrasse.pickel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Hexagonale Schichtregeln aus CLAUDE.md und .claude/rules/java.md als Build-Gate. Ein Agent verantwortet nur die
 * Änderung vor sich und fühlt sich keiner Schichtgrenze verpflichtet; jede Abkürzung wirkt einzeln harmlos, in Summe
 * zerfällt die Architektur. Deshalb prüft das hier der Build und nicht nur ein Reviewer.
 */
@AnalyzeClasses(packages = "ch.grudligstrasse.pickel", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "ch.grudligstrasse.pickel.domain..";
    private static final String DRIVING_PORTS = "ch.grudligstrasse.pickel.domain.port.in..";
    private static final String DRIVEN_PORTS = "ch.grudligstrasse.pickel.domain.port.out..";
    private static final String PORTS = "ch.grudligstrasse.pickel.domain.port..";
    private static final String APPLICATION = "ch.grudligstrasse.pickel.application..";
    private static final String ADAPTER = "ch.grudligstrasse.pickel.adapter..";
    private static final String REST_ADAPTER = "ch.grudligstrasse.pickel.adapter.in.rest..";
    private static final String PERSISTENCE_ADAPTER = "ch.grudligstrasse.pickel.adapter.out.persistence..";

    @ArchTest
    static final ArchRule dependencies_point_inwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .ensureAllClassesAreContainedInArchitecture()
            .layer("Domain")
            .definedBy(DOMAIN)
            .layer("Application")
            .definedBy(APPLICATION)
            .layer("Adapter")
            .definedBy(ADAPTER)
            .whereLayer("Adapter")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Adapter")
            .because("ein neues Paket eine bewusste Entscheidung über seine Schicht ist");

    @ArchTest
    static final ArchRule domain_depends_only_on_java_and_itself = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN)
            .because("die Fachlogik ohne Framework testbar und austauschbar bleiben soll");

    @ArchTest
    static final ArchRule ports_are_interfaces = classes()
            .that()
            .resideInAPackage(PORTS)
            .should()
            .beInterfaces()
            .because("Ports Verträge sind, die Application und Adapter implementieren");

    @ArchTest
    static final ArchRule application_services_implement_driving_ports = classes()
            .that()
            .resideInAPackage(APPLICATION)
            .and()
            .haveSimpleNameEndingWith("Service")
            .should()
            .implement(DescribedPredicate.describe(
                    "ein Interface aus domain.port.in",
                    javaClass -> javaClass.getPackageName().startsWith("ch.grudligstrasse.pickel.domain.port.in")))
            .because("REST-Resources nur über Use-Case-Ports auf die Application zugreifen");

    @ArchTest
    static final ArchRule jpa_entities_only_in_persistence_adapter = classes()
            .that()
            .areAnnotatedWith(Entity.class)
            .should()
            .resideInAPackage(PERSISTENCE_ADAPTER)
            .andShould()
            .haveSimpleNameEndingWith("Entity")
            .because("das Mapping Entity ↔ Domain in den Persistenz-Adapter gehört");

    @ArchTest
    static final ArchRule jpa_and_panache_only_in_persistence_adapter = noClasses()
            .that()
            .resideOutsideOfPackage(PERSISTENCE_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "io.quarkus.hibernate..", "org.hibernate..")
            .because("ein Schemawechsel nur den Persistenz-Adapter berühren soll");

    @ArchTest
    static final ArchRule persistence_adapter_does_not_know_rest_adapter = noClasses()
            .that()
            .resideInAPackage(PERSISTENCE_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(REST_ADAPTER)
            .because("DTOs nur im REST-Adapter gemappt werden");

    @ArchTest
    static final ArchRule rest_adapter_does_not_know_driven_ports = noClasses()
            .that()
            .resideInAPackage(REST_ADAPTER)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(PERSISTENCE_ADAPTER, DRIVEN_PORTS)
            .because("REST-Resources an Use-Case-Ports delegieren und keine Repositories kennen");

    @ArchTest
    static final ArchRule rest_resources_live_in_rest_adapter = classes()
            .that()
            .areAnnotatedWith(Path.class)
            .should()
            .resideInAPackage(REST_ADAPTER)
            .andShould()
            .haveSimpleNameEndingWith("Resource")
            .because("HTTP nur im REST-Adapter ankommt");

    @ArchTest
    static final ArchRule rest_path_params_are_not_uuid = RestAdapterRules.noUuidPathParams();

    @ArchTest
    static final ArchRule exception_mappers_live_in_rest_adapter = classes()
            .that()
            .implement(ExceptionMapper.class)
            .should()
            .resideInAPackage(REST_ADAPTER)
            .because("HTTP-Status nur im REST-Adapter aus Domain-Exceptions entstehen");

    @ArchTest
    static final ArchRule http_types_only_in_rest_adapter = noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("jakarta.ws.rs..")
            .because("die Application keine HTTP-Details kennt");

    @ArchTest
    static final ArchRule transactions_only_in_application_classes = noClasses()
            .that()
            .resideOutsideOfPackage(APPLICATION)
            .should()
            .beAnnotatedWith(Transactional.class)
            .because("eine Transaktion genau einen Use Case umfasst");

    @ArchTest
    static final ArchRule transactions_only_in_application_methods = methods()
            .that()
            .areAnnotatedWith(Transactional.class)
            .should()
            .beDeclaredInClassesThat()
            .resideInAPackage(APPLICATION)
            .because("eine Transaktion genau einen Use Case umfasst");

    @ArchTest
    static final ArchRule driving_ports_are_implemented_only_by_application = classes()
            .that()
            .resideInAPackage(DRIVING_PORTS)
            .should()
            .onlyHaveDependentClassesThat()
            .resideInAnyPackage(DRIVING_PORTS, APPLICATION, REST_ADAPTER)
            .because("Use Cases nur von der Application umgesetzt und vom REST-Adapter aufgerufen werden");
}
