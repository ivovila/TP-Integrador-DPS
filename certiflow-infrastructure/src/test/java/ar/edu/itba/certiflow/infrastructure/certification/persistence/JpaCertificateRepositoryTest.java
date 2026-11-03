package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.model.CertificationAssessmentContext;
import ar.edu.itba.certiflow.certification.model.ClosedAt;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.MonthsTerm;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.certification.model.PolicyReference;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.SeverityCertificationPolicy;
import ar.edu.itba.certiflow.certification.model.StandardFindingTreatment;
import ar.edu.itba.certiflow.certification.model.WholeAssetScope;
import ar.edu.itba.certiflow.infrastructure.inspection.persistence.JpaInspectionRepository;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import ar.edu.itba.certiflow.infrastructure.support.PersistenceMappers;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaCertificateRepository.class, SequenceCertificateNumbering.class, JpaInspectionRepository.class,
        JpaInspectionSchemaRepository.class, PersistenceMappers.class})
class JpaCertificateRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    @Autowired
    private JpaCertificateRepository repository;
    @Autowired
    private SequenceCertificateNumbering numbering;
    @Autowired
    private JpaInspectionRepository inspections;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;

    private Certificate issued() {
        return issuedUnder(DomainSamples.standardPolicy());
    }

    private Certificate issuedUnder(SeverityCertificationPolicy policy) {
        var schema = DomainSamples.schema();
        schemas.add(schema);
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        DomainSamples.close(inspection);
        inspections.save(inspection);
        InspectionResult result = new InspectionResult(inspection.id(), inspection.assetCode(), new ClosedAt(inspection.originalRevision().sealedAt()), true, List.of(), new SchemaReference(new AssetType("Fire extinguisher"), 1), List.of(), new WholeAssetScope());
        return new CertificateFactory().create(numbering.next(), result, policy.evaluate(
                new CertificationAssessmentContext(result,
                        new JurisdictionCode("DEMO-STANDARD"), DomainSamples.T0, TODAY)).ensureGranted(),
                new Stamp(DomainSamples.PLANNER, DomainSamples.T0));
    }

    @Test
    void aCertificateIsReadBackWithTheSameSnapshotAndIsCurrent() {
        Certificate certificate = issued();
        repository.save(certificate);
        entityManager.flush();
        entityManager.clear();

        assertEquals(certificate.snapshot(), repository.findByNumber(certificate.number()).orElseThrow().snapshot());
        assertEquals(certificate.number(), repository.findByAsset(certificate.assetCode()).stream().filter(found -> found.isCurrentFor(certificate.scope(), TODAY)).findFirst().orElseThrow().number());
        assertTrue(repository.findByAsset(certificate.assetCode()).stream().filter(found -> found.isCurrentFor(certificate.scope(), TODAY.plusYears(2))).findFirst().isEmpty());
        assertEquals(1, repository.findByAsset(certificate.assetCode()).size());
    }

    @Test
    void theDecisionIsReadBackWithTheVersionOfThePolicyThatTookIt() {
        SeverityCertificationPolicy secondVersion = new SeverityCertificationPolicy(
                new PolicyReference(new PolicyCode("STANDARD"), 2), Map.of(StandardSeverity.MINOR, StandardFindingTreatment.TOLERATED,
                StandardSeverity.MAJOR, StandardFindingTreatment.BLOCKS, StandardSeverity.CRITICAL, StandardFindingTreatment.BLOCKS),
                new MonthsTerm(6));
        Certificate certificate = issuedUnder(secondVersion);
        repository.save(certificate);
        entityManager.flush();
        entityManager.clear();

        Certificate restored = repository.findByNumber(certificate.number()).orElseThrow();

        assertEquals(new PolicyReference(new PolicyCode("STANDARD"), 2), restored.decision().policy());
        assertEquals(TODAY.plusMonths(6).minusDays(1), restored.validity().validUntil());
    }

    @Test
    void numbersAreConsecutiveAndFormatted() {
        String first = numbering.next().value();
        String second = numbering.next().value();

        assertTrue(first.matches("CERT-\\d{5}"));
        assertEquals(Integer.parseInt(first.substring(5)) + 1, Integer.parseInt(second.substring(5)));
    }

    @Test
    void aSuspendedCertificateIsReadBackSuspended() {
        CertificateService service = new CertificateService(new CertificateFactory(), mock(AuditLog.class));
        Certificate certificate = issued();
        service.suspend(certificate, "Seal tampered", new Stamp(DomainSamples.PLANNER, DomainSamples.T0.plusSeconds(60)), TODAY);
        repository.save(certificate);
        entityManager.flush();
        entityManager.clear();

        Certificate restored = repository.findByNumber(certificate.number()).orElseThrow();

        assertEquals(certificate.snapshot(), restored.snapshot());
        assertEquals(CertificateStatus.SUSPENDED, restored.statusOn(TODAY, List.of()));
    }
}
