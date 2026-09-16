package ar.edu.itba.certiflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.domain.Certificate;
import ar.edu.itba.certiflow.domain.CorrectiveAction;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Inspection;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

@org.junit.jupiter.api.Tag("integration")
class CertificationWorkflowTest {
    @Test
    void completeFlowProducesConsistentBusinessReportsAndAudit() {
        var f = new Fixture();
        var i = f.closed("60");
        assertThrows(DomainException.class, () -> f.certificates.issue(i.id(), "issuer"));
        f.resolve(i);
        var certificate = f.certificates.issue(i.id(), "issuer");
        var act = f.reports.inspectionAct(i.id());
        var summary = f.reports.findings(i.id());
        var report = f.reports.certificate(certificate.id());
        assertEquals(Inspection.State.CLOSED, act.inspection().state());
        assertEquals(3, act.inspection().evaluations().size());
        assertEquals(1, summary.findings().size());
        assertEquals(CorrectiveAction.State.CLOSED, summary.actions().get(0).state());
        assertEquals(Certificate.Status.ACTIVE, report.status());
        assertEquals(f.asset, report.asset());
        assertEquals(1, report.schemeVersion());
        assertEquals("ISSUED", certificate.history().get(0).operation());
    }

    @Test
    void expiryIsEffectiveAtExactBoundaryWithoutBackgroundJob() {
        var f = new Fixture();
        var c = f.certificates.issue(f.closed("20").id(), "issuer");
        f.clock.advance(Duration.ofDays(365).minusNanos(1));
        assertEquals(Certificate.Status.ACTIVE, f.certificates.status(c.id()));
        f.clock.advance(Duration.ofNanos(1));
        assertEquals(Certificate.Status.EXPIRED, f.certificates.status(c.id()));
        assertEquals(Certificate.Status.EXPIRED, f.reports.certificate(c.id()).status());
        assertThrows(
                DomainException.class, () -> f.certificates.suspend(c.id(), "Reason", "issuer"));
    }

    @Test
    void suspensionRequiresReasonAndIsAudited() {
        var f = new Fixture();
        var c = f.certificates.issue(f.closed("20").id(), "issuer");
        assertThrows(DomainException.class, () -> f.certificates.suspend(c.id(), " ", "issuer"));
        var suspended = f.certificates.suspend(c.id(), "Equipment recalled", "issuer");
        assertEquals(Certificate.Status.SUSPENDED, f.certificates.status(c.id()));
        assertEquals("Equipment recalled", suspended.history().get(1).detail());
    }

    @Test
    void rectificationImmediatelySuspendsCertificateAndReclosureDoesNotReactivateIt() {
        var f = new Fixture();
        var i = f.closed("20");
        var c = f.certificates.issue(i.id(), "issuer");
        f.inspections.rectify(i.id(), "Correct observations", "supervisor");
        assertEquals(Certificate.Status.SUSPENDED, f.certificates.status(c.id()));
        assertTrue(f.reports.certificate(c.id()).statusReason().contains("rectified"));
        f.inspections.close(i.id(), "inspector");
        assertEquals(Certificate.Status.SUSPENDED, f.certificates.status(c.id()));
        var replacement = f.certificates.issue(i.id(), "issuer");
        assertEquals(2, replacement.inspectionRevision());
        assertEquals(1, c.inspectionRevision());
    }

    @Test
    void cannotIssueTwiceForSameRevisionOrHaveTwoActiveCertificatesForAsset() {
        var f = new Fixture();
        var i = f.closed("20");
        f.certificates.issue(i.id(), "issuer");
        assertThrows(DomainException.class, () -> f.certificates.issue(i.id(), "issuer"));
        var another = f.closed("20");
        assertThrows(DomainException.class, () -> f.certificates.issue(another.id(), "issuer"));
        assertEquals(1, f.certificateRepo.findByAsset(f.asset.id()).size());
    }

    @Test
    void renewalRequiresNewInspectionAndSupersedesOldCertificateWithReference() {
        var f = new Fixture();
        var i = f.closed("20");
        var old = f.certificates.issue(i.id(), "issuer");
        assertThrows(DomainException.class, () -> f.certificates.renew(old.id(), i.id(), "issuer"));
        f.clock.advance(Duration.ofDays(1));
        var newer = f.closed("20");
        var replacement = f.certificates.renew(old.id(), newer.id(), "issuer");
        assertEquals(Certificate.Status.SUPERSEDED, f.certificates.status(old.id()));
        assertEquals(Certificate.Status.ACTIVE, f.certificates.status(replacement.id()));
        assertTrue(
                f.certificates
                        .get(old.id())
                        .history()
                        .get(1)
                        .detail()
                        .contains(replacement.id().toString()));
        assertThrows(
                DomainException.class,
                () -> f.certificates.renew(old.id(), f.closed("20").id(), "issuer"));
    }

    @Test
    void renewalFailureDoesNotModifyOldCertificate() {
        var f = new Fixture();
        var old = f.certificates.issue(f.closed("20").id(), "issuer");
        var rejected = f.closed("60");
        assertThrows(
                DomainException.class,
                () -> f.certificates.renew(old.id(), rejected.id(), "issuer"));
        assertSame(old, f.certificates.get(old.id()));
        assertEquals(1, f.certificateRepo.findByAsset(f.asset.id()).size());
    }

    @Test
    void cannotIssueOrProduceFinalActWhileInspectionIsOpen() {
        var f = new Fixture();
        var i = f.started();
        assertThrows(DomainException.class, () -> f.certificates.issue(i.id(), "issuer"));
        assertThrows(DomainException.class, () -> f.reports.inspectionAct(i.id()));
    }

    @Test
    void directDomainFactoryCannotBypassFindingResolution() {
        var f = new Fixture();
        var inspection = f.closed("60");
        assertThrows(
                DomainException.class,
                () ->
                        Certificate.issue(
                                java.util.UUID.randomUUID(),
                                inspection,
                                java.util.List.of(),
                                "issuer",
                                f.clock.instant()));
    }

    @Test
    void renewalRejectsInspectionThatPredatesOriginalIssuance() {
        var f = new Fixture();
        var earlier = f.closed("20");
        f.clock.advance(Duration.ofDays(1));
        var old = f.certificates.issue(f.closed("20").id(), "issuer");
        assertThrows(
                DomainException.class,
                () -> f.certificates.renew(old.id(), earlier.id(), "issuer"));
        assertEquals(Certificate.Status.ACTIVE, f.certificates.status(old.id()));
    }

    @Test
    void expiredCertificateCanBeRenewedWithFreshInspection() {
        var f = new Fixture();
        var old = f.certificates.issue(f.closed("20").id(), "issuer");
        f.clock.advance(Duration.ofDays(366));
        var replacement = f.certificates.renew(old.id(), f.closed("20").id(), "issuer");
        assertEquals(Certificate.Status.SUPERSEDED, f.certificates.status(old.id()));
        assertEquals(Certificate.Status.ACTIVE, f.certificates.status(replacement.id()));
    }

    @Test
    void renewalCannotUseAnInspectionOfAnotherAsset() {
        var f = new Fixture();
        var old = f.certificates.issue(f.closed("20").id(), "issuer");
        var other =
                f.catalog.registerAsset(
                        "Otro laboratorio", "LAB", "Edificio 2", "owner", java.util.Map.of());
        var assigned =
                f.inspections.assign(
                        other.id(),
                        f.schemeId,
                        "inspector",
                        LocalDate.now(f.clock),
                        "Todos",
                        "coordinator");
        f.inspections.start(assigned.id(), "inspector");
        assertThrows(
                DomainException.class,
                () -> f.certificates.renew(old.id(), assigned.id(), "issuer"));
        assertSame(old, f.certificates.get(old.id()));
    }
}
