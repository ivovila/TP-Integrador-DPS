package ar.edu.itba.certiflow.certification.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.InSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalScopeMismatchException;
import ar.edu.itba.certiflow.certification.usecase.ExplicitRenewalScope;
import ar.edu.itba.certiflow.certification.usecase.SameScopeAsCurrent;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CertificationScopeTest {

    private static final SubsystemCode ELECTRICAL = new SubsystemCode("ELEC");
    private static final SubsystemCode PRESSURE = new SubsystemCode("PRES");
    private static final OpenNonConformity ELECTRICAL_FINDING =
            new OpenNonConformity("GROUNDING", StandardSeverity.MAJOR, "F-1", new InSubsystem(ELECTRICAL));

    private final InspectionResult wholeAssetResult = new InspectionResult(new InspectionId("I-1"), new AssetCode("A-1"),
            new ClosedAt(Instant.parse("2026-01-31T10:00:00Z")), true, List.of(ELECTRICAL_FINDING),
            new SchemaReference(new AssetType("Boiler"), 1),
            List.of(new SubsystemResult(ELECTRICAL, true, List.of(ELECTRICAL_FINDING)),
                    new SubsystemResult(PRESSURE, false, List.of())),
            new WholeAssetScope());

    @Test
    void theWholeAssetScopeKeepsTheResultAsItIs() {
        assertSame(wholeAssetResult, wholeAssetResult.forScope(new WholeAssetScope()));
    }

    @Test
    void aSubsystemScopeNarrowsTheResultToThatSubsystem() {
        InspectionResult pressureResult = wholeAssetResult.forScope(new SubsystemScope(PRESSURE));

        assertEquals(new SubsystemScope(PRESSURE), pressureResult.scope());
        assertFalse(pressureResult.coversWholeSchema());
        assertTrue(pressureResult.openNonConformities().isEmpty());
    }

    @Test
    void anUnknownSubsystemCannotNarrowTheResult() {
        assertThrows(SubsystemNotFoundException.class,
                () -> wholeAssetResult.forScope(new SubsystemScope(new SubsystemCode("MISSING"))));
    }

    @Test
    void onlyTheWholeAssetOfASplitSchemaRequiresPartialCertificates() {
        assertTrue(wholeAssetResult.requiresPartialCertificates());
        assertFalse(wholeAssetResult.forScope(new SubsystemScope(ELECTRICAL)).requiresPartialCertificates());
        assertFalse(new WholeAssetScope().requiresPartialCertificates(List.of()));
    }

    @Test
    void eachScopeDescribesItselfToItsReceiver() {
        assertEquals(List.of(), new WholeAssetScope().describedBy(new CertifiedSubsystems()));
        assertEquals(List.of(ELECTRICAL), new SubsystemScope(ELECTRICAL).describedBy(new CertifiedSubsystems()));
        assertEquals("whole asset", new WholeAssetScope().describedBy(new ScopeLabel()));
        assertEquals("subsystem ELEC", new SubsystemScope(ELECTRICAL).describedBy(new ScopeLabel()));
    }

    @Test
    void aRenewalKeepsTheCurrentScopeUnlessAnotherOneIsAskedFor() {
        SubsystemScope currentScope = new SubsystemScope(ELECTRICAL);

        assertEquals(currentScope, new SameScopeAsCurrent().resolveAgainst(currentScope));
        assertEquals(currentScope, new ExplicitRenewalScope(new SubsystemScope(ELECTRICAL)).resolveAgainst(currentScope));
        assertThrows(RenewalScopeMismatchException.class,
                () -> new ExplicitRenewalScope(new WholeAssetScope()).resolveAgainst(currentScope));
    }

    @Test
    void anInspectionBasisIsAlwaysSupportedAndDoesNotBackAGlobalCertificate() {
        Instant closedAt = Instant.parse("2026-01-31T10:00:00Z");
        InspectionBasis basis = new InspectionBasis(new InspectionReference(new InspectionId("I-1"), closedAt));

        assertEquals(closedAt, basis.latestInspectionClosedAt());
        assertTrue(basis.isSupportedOn(LocalDate.of(2026, 2, 1), List.of()));
        assertFalse(basis.backsGlobalCertificate());
    }

    @Test
    void aPartialCertificatesBasisIsNotSupportedWhenAPartialIsMissing() {
        Instant closedAt = Instant.parse("2026-01-31T10:00:00Z");
        PartialCertificatesBasis basis = new PartialCertificatesBasis(List.of(new CertificateNumber("CERT-00001")),
                "ALL-REQUIRED-SUBSYSTEMS", closedAt);

        assertEquals(closedAt, basis.latestInspectionClosedAt());
        assertFalse(basis.isSupportedOn(LocalDate.of(2026, 2, 1), List.of()));
        assertTrue(basis.backsGlobalCertificate());
    }

    @Test
    void onlyAnActiveCertificateBecomesInvalidWithoutActiveBacking() {
        assertEquals(CertificateStatus.DEPENDENCY_INVALID, CertificateStatus.ACTIVE.withoutActiveBacking());
        assertEquals(CertificateStatus.EXPIRED, CertificateStatus.EXPIRED.withoutActiveBacking());
        assertEquals(CertificateStatus.SUSPENDED, CertificateStatus.SUSPENDED.withoutActiveBacking());
        assertEquals(CertificateStatus.RENEWED, CertificateStatus.RENEWED.withoutActiveBacking());
    }
}
