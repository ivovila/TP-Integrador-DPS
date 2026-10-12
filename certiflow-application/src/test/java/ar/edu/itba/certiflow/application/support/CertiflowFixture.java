package ar.edu.itba.certiflow.application.support;

import ar.edu.itba.certiflow.application.certification.InspectionResultTranslatorImpl;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateCommand;
import ar.edu.itba.certiflow.inspection.model.Verification;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceCommand;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionCommand;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaCommand;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.catalog.RegisterAssetUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.EvaluateCertificationUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.IssueCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.RenewCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.SuspendCertificateUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.PlanCorrectiveActionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.VerifyCorrectiveActionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AddObservationUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AssignInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.AttachEvidenceUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.CloseInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.RecordAnswerUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.RectifyInspectionUseCaseImpl;
import ar.edu.itba.certiflow.application.certification.GenerateCertificateDocumentUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.GenerateFindingsSummaryUseCaseImpl;
import ar.edu.itba.certiflow.application.inspection.GenerateInspectionActUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.CreateInspectionSchemaUseCaseImpl;
import ar.edu.itba.certiflow.application.schema.PublishSchemaVersionUseCaseImpl;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryAssetRepository;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryAuditLog;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryCertificateRepository;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryFindingRepository;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryInspectionRepository;
import ar.edu.itba.certiflow.infrastructure.inmemory.InMemoryInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.inmemory.SequentialCertificateNumbering;
import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.certification.model.IneligibilityReason;
import ar.edu.itba.certiflow.certification.model.NoBlockingFindingsEligibility;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.NumericAnswer;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionAnswer;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceUseCase;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateFindingsSummaryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
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
    public final AuditLog<InspectionId> inspectionLog = new InMemoryAuditLog<>();
    public final AuditLog<FindingId> findingLog = new InMemoryAuditLog<>();
    public final AuditLog<CertificateNumber> certificateLog = new InMemoryAuditLog<>();
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
    public final RegisterAssetUseCase registerAsset = new RegisterAssetUseCaseImpl(assetRepository, assetService, clock);
    public final CreateInspectionSchemaUseCase createInspectionSchema = new CreateInspectionSchemaUseCaseImpl(inspectionSchemaRepository, inspectionSchemaService, clock);
    public final PublishSchemaVersionUseCase publishSchemaVersion = new PublishSchemaVersionUseCaseImpl(inspectionSchemaRepository, inspectionSchemaService, clock);
    public final AssignInspectionUseCase assignInspection = new AssignInspectionUseCaseImpl(assetRepository, inspectionSchemaRepository, inspectionRepository,
            inspectionService, clock);
    public final RecordAnswerUseCase recordAnswer = new RecordAnswerUseCaseImpl(inspectionRepository, inspectionService, clock);
    public final AttachEvidenceUseCase attachEvidence = new AttachEvidenceUseCaseImpl(inspectionRepository, inspectionService, clock);
    public final AddObservationUseCase addObservation = new AddObservationUseCaseImpl(inspectionRepository, inspectionService, clock);
    public final CloseInspectionUseCase closeInspection = new CloseInspectionUseCaseImpl(inspectionRepository, findingRepository, inspectionService, findingService, clock);
    public final RectifyInspectionUseCase rectifyInspection = new RectifyInspectionUseCaseImpl(inspectionRepository, findingRepository, inspectionService, findingService, clock);
    public final PlanCorrectiveActionUseCase planCorrectiveAction = new PlanCorrectiveActionUseCaseImpl(findingRepository, findingService, clock);
    public final VerifyCorrectiveActionUseCase verifyCorrectiveAction = new VerifyCorrectiveActionUseCaseImpl(findingRepository, findingService, clock);
    public final GenerateInspectionActUseCase generateInspectionAct = new GenerateInspectionActUseCaseImpl(inspectionRepository, assetRepository, inspectionLog);
    public final GenerateFindingsSummaryUseCase generateFindingsSummary = new GenerateFindingsSummaryUseCaseImpl(assetRepository, findingRepository, clock);
    public final GenerateCertificateDocumentUseCase generateCertificateDocument = new GenerateCertificateDocumentUseCaseImpl(certificateRepository, assetRepository, clock);

    private final CertificationEligibility certificationEligibility = new NoBlockingFindingsEligibility();
    private final InspectionResultTranslator inspectionResultTranslator = new InspectionResultTranslatorImpl(inspectionRepository,
            findingRepository);
    private final SequentialCertificateNumbering certificateNumbering = new SequentialCertificateNumbering();
    public final IssueCertificateUseCase issueCertificate = new IssueCertificateUseCaseImpl(inspectionResultTranslator, certificateRepository, certificationEligibility, certificateNumbering, certificateService, clock);
    public final EvaluateCertificationUseCase evaluateCertification = new EvaluateCertificationUseCaseImpl(inspectionResultTranslator, certificationEligibility);
    public final SuspendCertificateUseCase suspendCertificate = new SuspendCertificateUseCaseImpl(certificateRepository, certificateService, clock);
    public final RenewCertificateUseCase renewCertificate = new RenewCertificateUseCaseImpl(inspectionResultTranslator, certificateRepository, certificationEligibility, certificateNumbering, certificateService, clock);

    public final Asset extinguisher = registerAsset.execute(new RegisterAssetCommand(new AssetCode("EXT-0042"),
            "Lobby extinguisher", EXTINGUISHER, new Location("Main building, ground floor lobby"), assetResponsible,
            new Characteristics(List.of(new Characteristic("capacity", "10 kg"), new Characteristic("agent", "ABC powder"))), planner));

    public CertiflowFixture() {
        createInspectionSchema.execute(new CreateInspectionSchemaCommand("Portable extinguisher inspection", EXTINGUISHER,
                firstVersionSections(), planner));
    }

    public List<Section> firstVersionSections() {
        return List.of(new Section(PRESSURE_AND_SEAL, List.of(pressureCriterion, sealIntactCriterion)),
                new Section(SIGNAGE, List.of(signageCriterion)));
    }

    public InspectionId assignedInspection() {
        return assignedInspection(new FullScope());
    }

    public InspectionId assignedInspection(InspectionScope scope) {
        return assignInspection.execute(new AssignInspectionCommand(extinguisher.code(), inspector, TODAY.plusDays(3), scope, planner));
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
        answer(inspection, pressureCriterion, numericAnswer(pressureInBars), inspector);
        attach(inspection, pressureCriterion, gaugePhoto(), inspector);
        answer(inspection, sealIntactCriterion, sealAnswer, inspector);
        answer(inspection, signageCriterion, new OptionAnswer(signageOption), inspector);
    }

    public InspectionId closedCompliantInspection() {
        InspectionId inspection = assignedInspection();
        answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");
        close(inspection, inspector);
        return inspection;
    }

    /** Cierra un hallazgo por el camino del negocio: una acción correctiva verificada y aceptada. */
    public void closeFinding(FindingId finding) {
        CorrectiveActionId action = plan(finding, "Recharge the extinguisher", assetResponsible, TODAY.plusDays(10), planner);
        verify(finding, action, verifier, VerificationResult.ACCEPTED, "Recharged");
    }

    public FindingId majorFindingOf(InspectionId inspection) {
        answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        return close(inspection, inspector).getFirst();
    }

    public void answer(InspectionId inspection, Criterion<?> criterion, Answer answer, Person by) {
        recordAnswer.execute(new RecordAnswerCommand(inspection, criterion.code(), answer.raw(), by));
    }

    public void attach(InspectionId inspection, Criterion<?> criterion, Evidence evidence, Person by) {
        attachEvidence.execute(new AttachEvidenceCommand(inspection, criterion.code(), evidence, by));
    }

    public List<FindingId> close(InspectionId inspection, Person by) {
        return closeInspection.execute(new CloseInspectionCommand(inspection, by));
    }

    public List<FindingId> rectify(InspectionId inspection, Criterion<?> criterion, Answer answer, String reason, Person by) {
        return rectifyInspection.execute(new RectifyInspectionCommand(inspection, criterion.code(), answer.raw(), reason, by));
    }

    public CorrectiveActionId plan(FindingId finding, String description, Person responsible, LocalDate dueDate, Person by) {
        return planCorrectiveAction.execute(new PlanCorrectiveActionCommand(finding, description, responsible, dueDate, by));
    }

    public Verification verify(FindingId finding, CorrectiveActionId action, Person verifier, VerificationResult result, String notes) {
        return verifyCorrectiveAction.execute(new VerifyCorrectiveActionCommand(finding, action, verifier, result, notes));
    }

    public CertificateNumber issue(InspectionId inspection, LocalDate validUntil, Person by) {
        return issueCertificate.execute(new IssueCertificateCommand(inspection, validUntil, by));
    }

    public CertificateNumber renew(CertificateNumber current, InspectionId inspection, LocalDate validUntil, Person by) {
        return renewCertificate.execute(new RenewCertificateCommand(current, inspection, validUntil, by));
    }

    public void suspend(CertificateNumber number, String reason, Person by) {
        suspendCertificate.execute(new SuspendCertificateCommand(number, reason, by));
    }
}
