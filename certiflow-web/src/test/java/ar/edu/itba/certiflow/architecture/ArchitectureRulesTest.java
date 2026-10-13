package ar.edu.itba.certiflow.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.DomainException;
import ar.edu.itba.certiflow.shared.model.InvalidValueException;
import ar.edu.itba.certiflow.shared.model.NotFoundException;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Las reglas de dependencia que DESIGN.md declara, escritas para que el build falle si alguien las rompe.
 * Los paquetes de dominio son los de cada contexto; `application`, `infrastructure` y `web` son las otras capas.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureRulesTest {

    private static final String SHARED = "ar.edu.itba.certiflow.shared..";
    private static final String CATALOG = "ar.edu.itba.certiflow.catalog..";
    private static final String SCHEMA = "ar.edu.itba.certiflow.schema..";
    private static final String INSPECTION = "ar.edu.itba.certiflow.inspection..";
    private static final String CERTIFICATION = "ar.edu.itba.certiflow.certification..";
    private static final String APPLICATION = "ar.edu.itba.certiflow.application..";
    private static final String INFRASTRUCTURE = "ar.edu.itba.certiflow.infrastructure..";
    private static final String WEB = "ar.edu.itba.certiflow.web..";

    private final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ar.edu.itba.certiflow");

    @Test
    void theDomainDependsOnNoOtherLayer() {
        noClasses().that().resideInAnyPackage(SHARED, CATALOG, SCHEMA, INSPECTION, CERTIFICATION)
                .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE, WEB)
                .check(productionClasses);
    }

    @Test
    void theApplicationDependsOnNeitherInfrastructureNorWeb() {
        noClasses().that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, WEB)
                .check(productionClasses);
    }

    @Test
    void theDomainAndTheApplicationUseNoFramework() {
        noClasses().that().resideInAnyPackage(SHARED, CATALOG, SCHEMA, INSPECTION, CERTIFICATION, APPLICATION)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "tools.jackson..",
                        "com.fasterxml..", "org.hibernate..")
                .check(productionClasses);
    }

    @Test
    void theSharedKernelImportsNoContext() {
        noClasses().that().resideInAPackage(SHARED)
                .should().dependOnClassesThat().resideInAnyPackage(CATALOG, SCHEMA, INSPECTION, CERTIFICATION)
                .check(productionClasses);
    }

    @Test
    void theCatalogOnlyKnowsTheSharedKernel() {
        noClasses().that().resideInAPackage(CATALOG)
                .should().dependOnClassesThat().resideInAnyPackage(SCHEMA, INSPECTION, CERTIFICATION)
                .check(productionClasses);
    }

    @Test
    void theSchemaKnowsNeitherInspectionNorCertification() {
        noClasses().that().resideInAPackage(SCHEMA)
                .should().dependOnClassesThat().resideInAnyPackage(INSPECTION, CERTIFICATION)
                .check(productionClasses);
    }

    @Test
    void inspectionDoesNotKnowCertification() {
        noClasses().that().resideInAPackage(INSPECTION)
                .should().dependOnClassesThat().resideInAPackage(CERTIFICATION)
                .check(productionClasses);
    }

    @Test
    void certificationKnowsAnInspectionOnlyByItsIdentifier() {
        noClasses().that().resideInAPackage(CERTIFICATION)
                .should().dependOnClassesThat(resideInAPackage(INSPECTION).and(not(equivalentTo(InspectionId.class))))
                .check(productionClasses);
    }

    @Test
    void onlyTheTranslatorOfCertificationReadsTheInspectionContext() {
        noClasses().that().resideInAPackage("ar.edu.itba.certiflow.application.certification..")
                .and().doNotHaveSimpleName("InspectionResultTranslatorImpl")
                .should().dependOnClassesThat(resideInAPackage(INSPECTION).and(not(equivalentTo(InspectionId.class))))
                .check(productionClasses);
    }

    /** La categoría decide el estado HTTP: una excepción fuera de las tres no tendría quién la responda. */
    @Test
    void everyDomainExceptionBelongsToOneOfTheThreeCategories() {
        classes().that().areAssignableTo(DomainException.class)
                .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                .should().beAssignableTo(NotFoundException.class)
                .orShould().beAssignableTo(InvalidValueException.class)
                .orShould().beAssignableTo(RuleViolationException.class)
                .check(productionClasses);
    }
}
