package ar.edu.itba.certiflow.support;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.asset.RegisterAssetInteractor;
import ar.edu.itba.certiflow.application.certificate.EvaluateCertificationInteractor;
import ar.edu.itba.certiflow.application.certificate.IssueCertificateInteractor;
import ar.edu.itba.certiflow.application.certificate.RenewCertificateInteractor;
import ar.edu.itba.certiflow.application.certificate.SuspendCertificateInteractor;
import ar.edu.itba.certiflow.application.finding.PlanCorrectiveActionInteractor;
import ar.edu.itba.certiflow.application.finding.VerifyCorrectiveActionInteractor;
import ar.edu.itba.certiflow.application.inspection.AddObservationInteractor;
import ar.edu.itba.certiflow.application.inspection.AssignInspectionInteractor;
import ar.edu.itba.certiflow.application.inspection.AttachEvidenceInteractor;
import ar.edu.itba.certiflow.application.inspection.CloseInspectionInteractor;
import ar.edu.itba.certiflow.application.inspection.RecordAnswerInteractor;
import ar.edu.itba.certiflow.application.inspection.RectifyInspectionInteractor;
import ar.edu.itba.certiflow.application.report.GenerateCertificateDocumentInteractor;
import ar.edu.itba.certiflow.application.report.GenerateFindingsSummaryInteractor;
import ar.edu.itba.certiflow.application.report.GenerateInspectionActInteractor;
import ar.edu.itba.certiflow.application.schema.CreateInspectionSchemaInteractor;
import ar.edu.itba.certiflow.application.schema.PublishSchemaVersionInteractor;
import ar.edu.itba.certiflow.details.inmemory.InMemoryAssetRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryAuditLog;
import ar.edu.itba.certiflow.details.inmemory.InMemoryCertificateRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryFindingRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryInspectionRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryInspectionSchemaRepository;
import ar.edu.itba.certiflow.details.inmemory.SequentialCertificateNumbering;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.asset.Characteristic;
import ar.edu.itba.certiflow.models.asset.AssetService;
import ar.edu.itba.certiflow.models.asset.Characteristics;
import ar.edu.itba.certiflow.models.asset.Location;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateFactory;
import ar.edu.itba.certiflow.models.certificate.CertificateService;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certification.CertificationEligibility;
import ar.edu.itba.certiflow.models.certification.IneligibilityReason;
import ar.edu.itba.certiflow.models.certification.NoBlockingFindingsEligibility;
import ar.edu.itba.certiflow.models.certification.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.evaluation.EvidenceRequirement;
import ar.edu.itba.certiflow.models.evaluation.NumericAnswer;
import ar.edu.itba.certiflow.models.evaluation.NumericRangeRule;
import ar.edu.itba.certiflow.models.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.models.evaluation.OptionInListRule;
import ar.edu.itba.certiflow.models.evaluation.Range;
import ar.edu.itba.certiflow.models.evaluation.StandardEvidenceKind;
import ar.edu.itba.certiflow.models.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.models.evaluation.Unit;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.evaluation.YesNoRule;
import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingFactory;
import ar.edu.itba.certiflow.models.finding.FindingService;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.FullScope;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.InspectionFactory;
import ar.edu.itba.certiflow.models.inspection.InspectionService;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.InspectionScope;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.InspectionSchemaService;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.models.shared.PersonId;
import ar.edu.itba.certiflow.usecases.asset.RegisterAsset;
import ar.edu.itba.certiflow.usecases.certificate.EvaluateCertification;
import ar.edu.itba.certiflow.usecases.certificate.IssueCertificate;
import ar.edu.itba.certiflow.usecases.certificate.RenewCertificate;
import ar.edu.itba.certiflow.usecases.certificate.SuspendCertificate;
import ar.edu.itba.certiflow.usecases.finding.PlanCorrectiveAction;
import ar.edu.itba.certiflow.usecases.finding.VerifyCorrectiveAction;
import ar.edu.itba.certiflow.usecases.inspection.AddObservation;
import ar.edu.itba.certiflow.usecases.inspection.AssignInspection;
import ar.edu.itba.certiflow.usecases.inspection.AttachEvidence;
import ar.edu.itba.certiflow.usecases.inspection.CloseInspection;
import ar.edu.itba.certiflow.usecases.inspection.RecordAnswer;
import ar.edu.itba.certiflow.usecases.inspection.RectifyInspection;
import ar.edu.itba.certiflow.usecases.report.GenerateCertificateDocument;
import ar.edu.itba.certiflow.usecases.report.GenerateFindingsSummary;
import ar.edu.itba.certiflow.usecases.report.GenerateInspectionAct;
import ar.edu.itba.certiflow.usecases.schema.CreateInspectionSchema;
import ar.edu.itba.certiflow.usecases.schema.PublishSchemaVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.function.Executable;

public final class CertiflowFixture {

    public static final Unit BAR = new Unit("bar");
    public static final AssetType EXTINGUISHER = new AssetType("Fire extinguisher");
    public static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    public static final String PRESSURE_AND_SEAL = "Pressure and seal";
    public static final String SIGNAGE = "Signage";

    public final Person planner = new Person(new PersonId("EMP-001"), "Paula Planner");
    public final Person inspector = new Person(new PersonId("EMP-002"), "Ines Inspector");
    public final Person assetResponsible = new Person(new PersonId("EMP-003"), "Raul Responsible");
    public final Person maintenanceTechnician = new Person(new PersonId("EMP-004"), "Marta Maintenance");
    public final Person verifier = new Person(new PersonId("EMP-005"), "Victor Verifier");
    public final Person certifier = new Person(new PersonId("EMP-006"), "Carla Certifier");

    public final Criterion<NumericAnswer> pressureCriterion = new Criterion<>("EXT-01", "Gauge pressure within range",
            new NumericRangeRule(BAR, new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
            StandardSeverity.MAJOR, List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1)));
    public final Criterion<YesNoAnswer> sealIntactCriterion = new Criterion<>("EXT-02", "Safety seal is intact",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.CRITICAL, List.of());
    public final Criterion<OptionAnswer> signageCriterion = new Criterion<>("EXT-03", "Signage condition",
            new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE")), StandardSeverity.MINOR, List.of());

    public final MutableClock clock = new MutableClock(Instant.parse("2026-03-10T09:00:00Z"));
    public final AuditLog<AssetType> schemaLog = new InMemoryAuditLog<>();
    public final AuditLog<Inspection> inspectionLog = new InMemoryAuditLog<>();
    public final AuditLog<Finding> findingLog = new InMemoryAuditLog<>();
    public final AuditLog<Certificate> certificateLog = new InMemoryAuditLog<>();
    public final InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
    public final InMemoryInspectionSchemaRepository inspectionSchemaRepository = new InMemoryInspectionSchemaRepository();
    public final InMemoryInspectionRepository inspectionRepository = new InMemoryInspectionRepository();
    public final InMemoryFindingRepository findingRepository = new InMemoryFindingRepository();
    public final InMemoryCertificateRepository certificateRepository = new InMemoryCertificateRepository();

    public final AuditLog<AssetCode> assetLog = new InMemoryAuditLog<>();
    private final InspectionSchemaService inspectionSchemaService = new InspectionSchemaService(schemaLog);
    private final AssetService assetService = new AssetService(assetLog);
    private final InspectionService inspectionService = new InspectionService(new InspectionFactory(), inspectionLog);
    private final FindingService findingService = new FindingService(new FindingFactory(), findingLog);
    private final CertificateService certificateService = new CertificateService(new CertificateFactory(), certificateLog);
    public final RegisterAsset registerAsset = new RegisterAssetInteractor(assetRepository, assetService, clock);
    public final CreateInspectionSchema createInspectionSchema = new CreateInspectionSchemaInteractor(inspectionSchemaRepository, inspectionSchemaService, clock);
    public final PublishSchemaVersion publishSchemaVersion = new PublishSchemaVersionInteractor(inspectionSchemaRepository, inspectionSchemaService, clock);
    public final AssignInspection assignInspection = new AssignInspectionInteractor(assetRepository, inspectionSchemaRepository, inspectionRepository,
            inspectionService, clock);
    public final RecordAnswer recordAnswer = new RecordAnswerInteractor(inspectionRepository, inspectionService, clock);
    public final AttachEvidence attachEvidence = new AttachEvidenceInteractor(inspectionRepository, inspectionService, clock);
    public final AddObservation addObservation = new AddObservationInteractor(inspectionRepository, inspectionService, clock);
    public final CloseInspection closeInspection = new CloseInspectionInteractor(inspectionRepository, findingRepository, inspectionService, findingService, clock);
    public final RectifyInspection rectifyInspection = new RectifyInspectionInteractor(inspectionRepository, findingRepository, inspectionService, findingService, clock);
    public final PlanCorrectiveAction planCorrectiveAction = new PlanCorrectiveActionInteractor(findingRepository, findingService, clock);
    public final VerifyCorrectiveAction verifyCorrectiveAction = new VerifyCorrectiveActionInteractor(findingRepository, findingService, clock);
    public final GenerateInspectionAct generateInspectionAct = new GenerateInspectionActInteractor(inspectionRepository, assetRepository, inspectionLog);
    public final GenerateFindingsSummary generateFindingsSummary = new GenerateFindingsSummaryInteractor(assetRepository, findingRepository, clock);
    public final GenerateCertificateDocument generateCertificateDocument = new GenerateCertificateDocumentInteractor(certificateRepository, assetRepository, clock);

    private final CertificationEligibility certificationEligibility = new NoBlockingFindingsEligibility();
    private final SequentialCertificateNumbering certificateNumbering = new SequentialCertificateNumbering();
    public final IssueCertificate issueCertificate = new IssueCertificateInteractor(inspectionRepository, certificateRepository, findingRepository, certificationEligibility, certificateNumbering, certificateService, clock);
    public final EvaluateCertification evaluateCertification = new EvaluateCertificationInteractor(inspectionRepository, findingRepository,
            certificationEligibility);
    public final SuspendCertificate suspendCertificate = new SuspendCertificateInteractor(certificateRepository, certificateService, clock);
    public final RenewCertificate renewCertificate = new RenewCertificateInteractor(inspectionRepository, certificateRepository, findingRepository, certificationEligibility, certificateNumbering, certificateService, clock);

    public final Asset extinguisher = registerAsset.execute(new Asset(new AssetCode("EXT-0042"),
            "Lobby extinguisher", EXTINGUISHER, new Location("Main building, ground floor lobby"), assetResponsible,
            new Characteristics(List.of(new Characteristic("capacity", "10 kg"), new Characteristic("agent", "ABC powder")))), planner);

    public CertiflowFixture() {
        createInspectionSchema.execute("Portable extinguisher inspection", EXTINGUISHER, firstVersionSections(), planner);
    }

    public List<Section> firstVersionSections() {
        return List.of(new Section(PRESSURE_AND_SEAL, List.of(pressureCriterion, sealIntactCriterion)),
                new Section(SIGNAGE, List.of(signageCriterion)));
    }

    public InspectionId assignedInspection() {
        return assignedInspection(new FullScope());
    }

    public InspectionId assignedInspection(InspectionScope scope) {
        return assignInspection.execute(extinguisher.code(), new InspectionAssignment(inspector, TODAY.plusDays(3), scope), planner);
    }

    public Inspection inspection(InspectionId id) {
        return inspectionRepository.findById(id).orElseThrow();
    }

    public Finding finding(FindingId id) {
        return findingRepository.findById(id).orElseThrow();
    }

    public CorrectiveAction action(FindingId findingId, CorrectiveActionId actionId) {
        return finding(findingId).actions().stream().filter(action -> action.id().equals(actionId)).findFirst().orElseThrow();
    }

    public Certificate certificate(CertificateNumber number) {
        return certificateRepository.findByNumber(number).orElseThrow();
    }

    public static void assertNotEligible(IneligibilityReason reason, Executable certification) {
        NotEligibleForCertificationException rejection = assertThrows(NotEligibleForCertificationException.class, certification);
        assertTrue(rejection.decision().isBlockedBy(reason), rejection.getMessage());
    }

    public NumericAnswer numericAnswer(String decimalValue) {
        return new NumericAnswer(new BigDecimal(decimalValue));
    }

    public Evidence gaugePhoto() {
        return new Evidence(StandardEvidenceKind.PHOTO, "photos/ext-0042-gauge.jpg", "Pressure gauge reading");
    }

    public void answerAll(InspectionId inspection, String pressureInBars, YesNoAnswer sealAnswer, String signageOption) {
        recordAnswer.execute(inspection, pressureCriterion, numericAnswer(pressureInBars), inspector);
        attachEvidence.execute(inspection, pressureCriterion, gaugePhoto(), inspector);
        recordAnswer.execute(inspection, sealIntactCriterion, sealAnswer, inspector);
        recordAnswer.execute(inspection, signageCriterion, new OptionAnswer(signageOption), inspector);
    }

    public InspectionId closedCompliantInspection() {
        InspectionId inspection = assignedInspection();
        answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");
        closeInspection.execute(inspection, inspector);
        return inspection;
    }

    /** Cierra un hallazgo por el camino del negocio: una acción correctiva verificada y aceptada. */
    public void closeFinding(FindingId finding) {
        CorrectiveActionId action = planCorrectiveAction.execute(finding, "Recharge the extinguisher", assetResponsible,
                TODAY.plusDays(10), planner);
        verifyCorrectiveAction.execute(finding, action, verifier, VerificationResult.ACCEPTED, "Recharged");
    }

    public FindingId majorFindingOf(InspectionId inspection) {
        answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        return closeInspection.execute(inspection, inspector).getFirst();
    }
}
