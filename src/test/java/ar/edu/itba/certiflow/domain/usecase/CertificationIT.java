package ar.edu.itba.certiflow.domain.usecase;

import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static ar.edu.itba.certiflow.support.Fixtures.SIGNAGE;
import static ar.edu.itba.certiflow.support.Fixtures.photo;
import static ar.edu.itba.certiflow.support.Fixtures.pressure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateStatus;
import ar.edu.itba.certiflow.domain.model.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.model.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.model.finding.CorrectiveActionPlanned;
import ar.edu.itba.certiflow.domain.model.finding.CorrectiveActionVerified;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingDetails;
import ar.edu.itba.certiflow.domain.model.finding.FindingClosed;
import ar.edu.itba.certiflow.domain.model.finding.FindingRaised;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import ar.edu.itba.certiflow.support.Fixtures;
import ar.edu.itba.certiflow.support.TestContext;

class CertificationIT {

    private TestContext ctx;
    private Asset asset;
    private InspectionSchema schema;
    private final PersonId responsible = PersonId.generate();
    private final PersonId verifier = PersonId.generate();

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        Asset template = Fixtures.extinguisher();
        asset = ctx.registerAsset.execute(template.getType(), template.getLocation(), responsible,
                template.getCharacteristics());
        schema = Fixtures.extinguisherSchema();
        ctx.schemas.save(schema);
        ctx.publishSchema.execute(schema.getId());
    }

    private InspectionId inspect(String bar) {
        Inspection inspection = ctx.assignInspection.execute(asset.getId(), schema.getId(), PersonId.generate(),
                ctx.clock.today(), "Relevamiento completo");
        InspectionId id = inspection.getId();
        ctx.startInspection.execute(id);
        ctx.registerResponse.execute(id, PRESSURE, r -> r.withMeasurement(pressure(bar)).withEvidence(photo()));
        ctx.registerResponse.execute(id, SIGNAGE, r -> r.withAnswer(true).withEvidence(photo()));
        return id;
    }

    private ValidityPeriod oneYear() {
        return ValidityPeriod.ofYears(ctx.clock.today(), 1);
    }

    @Test
    void openCriticalFindingBlocksTheCertificateUntilItIsCorrected() {
        InspectionId id = inspect("14");
        ctx.closeInspection.execute(id);
        Finding finding = ctx.raiseFinding.execute(id, new FindingDetails(PRESSURE, Severity.CRITICAL, "Presion por encima del maximo admitido", responsible));

        assertThrows(DomainException.class, () -> ctx.issueCertificate.execute(id, oneYear()));

        CorrectiveAction action = ctx.planAction.execute(finding.getId(), "Descargar y recalibrar", responsible,
                ctx.clock.today().plusDays(15));
        ctx.clock.advanceDays(7);
        ctx.verifyAction.execute(finding.getId(), action.getId(), verifier);
        ctx.closeFinding.execute(finding.getId());
        Certificate certificate = ctx.issueCertificate.execute(id, oneYear());

        assertEquals(CertificateStatus.ISSUED, certificate.getStatus());
        assertEquals(List.of(FindingRaised.class, CorrectiveActionPlanned.class, CorrectiveActionVerified.class,
                        FindingClosed.class),
                ctx.audit.history(finding.getId().value().toString()).stream().map(Object::getClass).toList());
    }

    @Test
    void observedCriteriaDoNotBlockTheCertificate() {
        InspectionId id = inspect("12.5");
        InspectionEvaluation evaluation = ctx.closeInspection.execute(id);

        Certificate certificate = ctx.issueCertificate.execute(id, oneYear());

        assertEquals(CriterionOutcome.OBSERVED, evaluation.overall());
        assertTrue(certificate.isValidOn(ctx.clock.today()));
    }

    @Test
    void rejectedCriterionNeedsAFindingBeforeCertifying() {
        InspectionId id = inspect("14");
        ctx.closeInspection.execute(id);

        assertThrows(DomainException.class, () -> ctx.issueCertificate.execute(id, oneYear()));
    }

    @Test
    void findingCannotBeRaisedTwiceForTheSameCriterion() {
        InspectionId id = inspect("14");
        ctx.closeInspection.execute(id);
        ctx.raiseFinding.execute(id, new FindingDetails(PRESSURE, Severity.MINOR, "Presion alta", responsible));

        assertThrows(DomainException.class,
                () -> ctx.raiseFinding.execute(id, new FindingDetails(PRESSURE, Severity.MAJOR, "Presion alta", responsible)));
    }

    @Test
    void overdueCorrectiveActionSuspendsTheCertificate() {
        InspectionId id = inspect("14");
        ctx.closeInspection.execute(id);
        Finding finding = ctx.raiseFinding.execute(id, new FindingDetails(PRESSURE, Severity.MINOR, "Presion levemente alta", responsible));
        ctx.planAction.execute(finding.getId(), "Ajustar valvula", responsible, ctx.clock.today().plusDays(30));
        Certificate certificate = ctx.issueCertificate.execute(id, oneYear());

        ctx.clock.advanceDays(30);
        assertTrue(ctx.suspendForOverdueActions.execute(asset.getId()).isEmpty());

        ctx.clock.advanceDays(1);
        List<Certificate> suspended = ctx.suspendForOverdueActions.execute(asset.getId());

        assertEquals(List.of(certificate), suspended);
        assertEquals(CertificateStatus.SUSPENDED, certificate.getStatus());
    }

    @Test
    void renewalRequiresANewPassingInspectionOfTheSameAsset() {
        InspectionId first = inspect("11");
        ctx.closeInspection.execute(first);
        Certificate current = ctx.issueCertificate.execute(first, oneYear());

        ctx.clock.advanceDays(350);
        InspectionId second = inspect("11.5");
        ctx.closeInspection.execute(second);
        Certificate renewed = ctx.renewCertificate.execute(current.getId(), second, oneYear());

        assertEquals(CertificateStatus.RENEWED, current.getStatus());
        assertEquals(renewed.getId(), current.getRenewedBy().orElseThrow());
        assertEquals(2, ctx.certificates.findByAsset(asset.getId()).size());

        ctx.clock.advanceDays(400);
        ctx.expireCertificate.execute(renewed.getId());
        assertEquals(CertificateStatus.EXPIRED, renewed.getStatus());
    }
}
