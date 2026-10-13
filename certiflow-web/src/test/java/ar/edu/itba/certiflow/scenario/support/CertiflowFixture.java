package ar.edu.itba.certiflow.scenario.support;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristic;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.IneligibilityReason;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationCommand;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.inspection.model.Verification;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceCommand;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceUseCase;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindFindingUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateFindingsSummaryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListFindingsUseCase;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionUseCase;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.NumericAnswer;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionAnswer;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaCommand;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.FindSchemaUseCase;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.function.Executable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;

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

    public final MutableClock clock;
    public final AuditLog<AssetCode> assetLog;
    public final AuditLog<AssetType> schemaLog;
    public final AuditLog<InspectionId> inspectionLog;
    public final AuditLog<FindingId> findingLog;
    public final AuditLog<CertificateNumber> certificateLog;
    public final AssetRepository assetRepository;
    public final InspectionSchemaRepository inspectionSchemaRepository;
    public final InspectionRepository inspectionRepository;
    public final FindingRepository findingRepository;
    public final CertificateRepository certificateRepository;
    public final RegisterAssetUseCase registerAsset;
    public final CreateInspectionSchemaUseCase createInspectionSchema;
    public final PublishSchemaVersionUseCase publishSchemaVersion;
    public final AssignInspectionUseCase assignInspection;
    public final RecordAnswerUseCase recordAnswer;
    public final AttachEvidenceUseCase attachEvidence;
    public final AddObservationUseCase addObservation;
    public final CloseInspectionUseCase closeInspection;
    public final RectifyInspectionUseCase rectifyInspection;
    public final PlanCorrectiveActionUseCase planCorrectiveAction;
    public final VerifyCorrectiveActionUseCase verifyCorrectiveAction;
    public final GenerateInspectionActUseCase generateInspectionAct;
    public final GenerateFindingsSummaryUseCase generateFindingsSummary;
    public final GenerateCertificateDocumentUseCase generateCertificateDocument;
    public final IssueCertificateUseCase issueCertificate;
    public final EvaluateCertificationUseCase evaluateCertification;
    public final SuspendCertificateUseCase suspendCertificate;
    public final RenewCertificateUseCase renewCertificate;
    public final FindAssetUseCase findAsset;
    public final ListAssetsUseCase listAssets;
    public final FindSchemaUseCase findSchema;
    public final FindInspectionUseCase findInspection;
    public final FindFindingUseCase findFinding;
    public final ListFindingsUseCase listFindings;
    public final ListCertificatesUseCase listCertificates;
    public final FindCertificateUseCase findCertificate;
    public final AuditHistoryUseCase auditHistory;
    public final Asset extinguisher;

    /** Arma el escenario sobre la aplicación real: la base tiene que estar vacía y el reloj en su inicio. */
    public CertiflowFixture(ApplicationContext context, MutableClock clock) {
        this.clock = clock;
        this.assetLog = auditLog(context, AssetCode.class);
        this.schemaLog = auditLog(context, AssetType.class);
        this.inspectionLog = auditLog(context, InspectionId.class);
        this.findingLog = auditLog(context, FindingId.class);
        this.certificateLog = auditLog(context, CertificateNumber.class);
        this.assetRepository = context.getBean(AssetRepository.class);
        this.inspectionSchemaRepository = context.getBean(InspectionSchemaRepository.class);
        this.inspectionRepository = context.getBean(InspectionRepository.class);
        this.findingRepository = context.getBean(FindingRepository.class);
        this.certificateRepository = context.getBean(CertificateRepository.class);
        this.registerAsset = context.getBean(RegisterAssetUseCase.class);
        this.createInspectionSchema = context.getBean(CreateInspectionSchemaUseCase.class);
        this.publishSchemaVersion = context.getBean(PublishSchemaVersionUseCase.class);
        this.assignInspection = context.getBean(AssignInspectionUseCase.class);
        this.recordAnswer = context.getBean(RecordAnswerUseCase.class);
        this.attachEvidence = context.getBean(AttachEvidenceUseCase.class);
        this.addObservation = context.getBean(AddObservationUseCase.class);
        this.closeInspection = context.getBean(CloseInspectionUseCase.class);
        this.rectifyInspection = context.getBean(RectifyInspectionUseCase.class);
        this.planCorrectiveAction = context.getBean(PlanCorrectiveActionUseCase.class);
        this.verifyCorrectiveAction = context.getBean(VerifyCorrectiveActionUseCase.class);
        this.generateInspectionAct = context.getBean(GenerateInspectionActUseCase.class);
        this.generateFindingsSummary = context.getBean(GenerateFindingsSummaryUseCase.class);
        this.generateCertificateDocument = context.getBean(GenerateCertificateDocumentUseCase.class);
        this.issueCertificate = context.getBean(IssueCertificateUseCase.class);
        this.evaluateCertification = context.getBean(EvaluateCertificationUseCase.class);
        this.suspendCertificate = context.getBean(SuspendCertificateUseCase.class);
        this.renewCertificate = context.getBean(RenewCertificateUseCase.class);
        this.findAsset = context.getBean(FindAssetUseCase.class);
        this.listAssets = context.getBean(ListAssetsUseCase.class);
        this.findSchema = context.getBean(FindSchemaUseCase.class);
        this.findInspection = context.getBean(FindInspectionUseCase.class);
        this.findFinding = context.getBean(FindFindingUseCase.class);
        this.listFindings = context.getBean(ListFindingsUseCase.class);
        this.listCertificates = context.getBean(ListCertificatesUseCase.class);
        this.findCertificate = context.getBean(FindCertificateUseCase.class);
        this.auditHistory = context.getBean(AuditHistoryUseCase.class);
        this.extinguisher = registerAsset.execute(new RegisterAssetCommand(new AssetCode("EXT-0042"), "Lobby extinguisher", EXTINGUISHER, new Location("Main building, ground floor lobby"), assetResponsible, new Characteristics(List.of(new Characteristic("capacity", "10 kg"), new Characteristic("agent", "ABC powder"))), planner, new JurisdictionCode("DEMO-STANDARD"), List.of()));
        createInspectionSchema.execute(new CreateInspectionSchemaCommand("Portable extinguisher inspection", EXTINGUISHER,
                firstVersionSections(), planner));
    }

    @SuppressWarnings("unchecked")
    private static <K> AuditLog<K> auditLog(ApplicationContext context, Class<K> key) {
        return (AuditLog<K>) context.getBeanProvider(ResolvableType.forClassWithGenerics(AuditLog.class, key)).getObject();
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

    public Optional<Certificate> currentWholeAssetCertificate(AssetCode asset, LocalDate date) {
        return certificateRepository.findByAsset(asset).stream()
                .filter(certificate -> certificate.isCurrentFor(new CertificationScope.WholeAsset(), date)).findFirst();
    }

    public Certificate certificate(CertificateNumber number) {
        return certificateRepository.findByNumber(number).orElseThrow();
    }

    public CertificationDecision evaluate(InspectionId inspection) {
        return evaluate(inspection, new CertificationScope.WholeAsset());
    }

    public CertificationDecision evaluate(InspectionId inspection, CertificationScope scope) {
        return evaluateCertification.execute(new EvaluateCertificationCommand(inspection, scope));
    }

    public static void assertNotEligible(IneligibilityReason reason, Executable certification) {
        NotEligibleForCertificationException rejection = assertThrows(NotEligibleForCertificationException.class, certification);
        assertTrue(isBlockedBy(rejection.decision(), reason), rejection.getMessage());
    }

    public static boolean isBlockedBy(CertificationDecision decision, IneligibilityReason reason) {
        return decision.ineligibilities().stream().anyMatch(ineligibility -> ineligibility.reason() == reason);
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

    public CertificateNumber issue(InspectionId inspection, Person by) {
        return issueCertificate.execute(new IssueCertificateCommand(inspection, by, new CertificationScope.WholeAsset()));
    }

    public CertificateNumber renew(CertificateNumber current, InspectionId inspection, Person by) {
        return renewCertificate.execute(new RenewCertificateCommand(current, inspection, by));
    }

    public void suspend(CertificateNumber number, String reason, Person by) {
        suspendCertificate.execute(new SuspendCertificateCommand(number, reason, by));
    }
}
