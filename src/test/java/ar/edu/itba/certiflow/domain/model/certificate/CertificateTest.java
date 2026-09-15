package ar.edu.itba.certiflow.domain.model.certificate;

import static ar.edu.itba.certiflow.support.Fixtures.NOW;
import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static ar.edu.itba.certiflow.support.Fixtures.pressureResponse;
import static ar.edu.itba.certiflow.support.Fixtures.signageWithPhoto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.support.Fixtures;

class CertificateTest {

    private final CertificationPolicy policy = new SeverityCertificationPolicy(Severity.MAJOR);
    private final Asset asset = Fixtures.extinguisher();
    private final InspectionSchema schema = Fixtures.extinguisherSchema();
    private final ValidityPeriod oneYear = ValidityPeriod.ofYears(NOW.toLocalDate(), 1);

    private Inspection approvedInspection() {
        return Fixtures.closedInspection(asset, schema, pressureResponse("11"), signageWithPhoto());
    }

    private Inspection rejectedInspection() {
        return Fixtures.closedInspection(asset, schema, pressureResponse("14"), signageWithPhoto());
    }

    private Certificate issued() {
        return Certificate.issue(CertificateId.generate(), approvedInspection(), List.of(), policy, oneYear, NOW);
    }

    private Finding findingOn(Inspection inspection, Severity severity) {
        return Finding.raise(FindingId.generate(), inspection, PRESSURE, severity, "Presion fuera de rango",
                PersonId.generate(), NOW);
    }

    @Test
    void openCriticalFindingPreventsIssuing() {
        Inspection inspection = rejectedInspection();
        List<Finding> findings = List.of(findingOn(inspection, Severity.CRITICAL));

        assertThrows(DomainException.class,
                () -> Certificate.issue(CertificateId.generate(), inspection, findings, policy, oneYear, NOW));
    }

    @Test
    void rejectedCriterionWithoutFindingPreventsIssuing() {
        Inspection inspection = rejectedInspection();

        assertThrows(DomainException.class,
                () -> Certificate.issue(CertificateId.generate(), inspection, List.of(), policy, oneYear, NOW));
    }

    @Test
    void openMinorFindingDoesNotPreventIssuing() {
        Inspection inspection = rejectedInspection();
        List<Finding> findings = List.of(findingOn(inspection, Severity.MINOR));

        Certificate certificate = Certificate.issue(CertificateId.generate(), inspection, findings, policy,
                oneYear, NOW);

        assertTrue(certificate.isValidOn(NOW.toLocalDate()));
    }

    @Test
    void suspendedCertificateIsNotValidUntilReinstated() {
        Certificate certificate = issued();

        certificate.suspend("Accion correctiva vencida", NOW);
        assertFalse(certificate.isValidOn(NOW.toLocalDate()));

        certificate.reinstate(NOW.plusDays(5));
        assertTrue(certificate.isValidOn(NOW.toLocalDate().plusDays(5)));
    }

    @Test
    void suspendedCertificateCannotBeRenewed() {
        Certificate certificate = issued();
        certificate.suspend("Accion correctiva vencida", NOW);

        assertThrows(InvalidTransitionException.class, () -> certificate.renew(CertificateId.generate(),
                approvedInspection(), List.of(), policy, oneYear, NOW));
    }

    @Test
    void expiredCertificateCannotBeSuspended() {
        Certificate certificate = issued();
        certificate.expire(NOW.plusYears(1).plusDays(1));

        assertEquals(CertificateStatus.EXPIRED, certificate.getStatus());
        assertThrows(InvalidTransitionException.class, () -> certificate.suspend("Motivo", NOW.plusYears(2)));
    }

    @Test
    void certificateCannotExpireWhileInForce() {
        Certificate certificate = issued();

        assertThrows(DomainException.class, () -> certificate.expire(NOW.plusMonths(6)));
    }

    @Test
    void renewalIssuesANewCertificateAndRetiresTheCurrentOne() {
        Certificate current = issued();
        CertificateId newId = CertificateId.generate();

        Certificate renewed = current.renew(newId, approvedInspection(), List.of(), policy,
                ValidityPeriod.ofYears(NOW.toLocalDate().plusYears(1), 1), NOW.plusMonths(11));

        assertEquals(CertificateStatus.RENEWED, current.getStatus());
        assertEquals(newId, current.getRenewedBy().orElseThrow());
        assertEquals(CertificateStatus.ISSUED, renewed.getStatus());
    }
}
