package ar.edu.itba.certiflow.support;

import ar.edu.itba.certiflow.application.certification.CertificationEligibility;
import ar.edu.itba.certiflow.application.certification.NoBlockingFindingsEligibility;
import ar.edu.itba.certiflow.details.inmemory.InMemoryAssetRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryAuditService;
import ar.edu.itba.certiflow.details.inmemory.InMemoryCertificateRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryFindingRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryInspectionRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryInspectionSchemaRepository;
import ar.edu.itba.certiflow.details.inmemory.SequentialCertificateNumbering;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.asset.Location;
import ar.edu.itba.certiflow.models.certificate.AuditedCertificateGenerator;
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
import ar.edu.itba.certiflow.models.finding.AuditedFindingGenerator;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.AuditedInspectionGenerator;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.usecases.asset.RegisterAsset;
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
import java.util.Map;
import java.util.Set;

public final class CertiflowFixture {

    public static final Unit BAR = new Unit("bar");
    public static final AssetType EXTINGUISHER = new AssetType("Fire extinguisher");
    public static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    public final Person planner = new Person("Paula Planner");
    public final Person inspector = new Person("Ines Inspector");
    public final Person assetResponsible = new Person("Raul Responsible");
    public final Person maintenanceTechnician = new Person("Marta Maintenance");
    public final Person verifier = new Person("Victor Verifier");
    public final Person certifier = new Person("Carla Certifier");

    public final Criterion<NumericAnswer> pressureCriterion = new Criterion<>("EXT-01", "Gauge pressure within range",
            new NumericRangeRule(BAR, new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
            StandardSeverity.MAJOR, List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1)));
    public final Criterion<YesNoAnswer> sealIntactCriterion = new Criterion<>("EXT-02", "Safety seal is intact",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.CRITICAL, List.of());
    public final Criterion<OptionAnswer> signageCriterion = new Criterion<>("EXT-03", "Signage condition",
            new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE")), StandardSeverity.MINOR, List.of());

    public final MutableClock clock = new MutableClock(Instant.parse("2026-03-10T09:00:00Z"));
    public final InMemoryAuditService auditService = new InMemoryAuditService();
    public final InMemoryAssetRepository assetRepository = new InMemoryAssetRepository();
    public final InMemoryInspectionSchemaRepository inspectionSchemaRepository = new InMemoryInspectionSchemaRepository();
    public final InMemoryInspectionRepository inspectionRepository = new InMemoryInspectionRepository();
    public final InMemoryFindingRepository findingRepository = new InMemoryFindingRepository();
    public final InMemoryCertificateRepository certificateRepository = new InMemoryCertificateRepository();

    public final RegisterAsset registerAsset = new RegisterAsset(assetRepository);
    public final CreateInspectionSchema createInspectionSchema = new CreateInspectionSchema(inspectionSchemaRepository, clock);
    public final PublishSchemaVersion publishSchemaVersion = new PublishSchemaVersion(inspectionSchemaRepository, clock);
    public final AssignInspection assignInspection = new AssignInspection(inspectionSchemaRepository, inspectionRepository,
            new AuditedInspectionGenerator(auditService), clock);
    public final RecordAnswer recordAnswer = new RecordAnswer(inspectionRepository, clock);
    public final AttachEvidence attachEvidence = new AttachEvidence(inspectionRepository, clock);
    public final AddObservation addObservation = new AddObservation(inspectionRepository, clock);
    public final CloseInspection closeInspection = new CloseInspection(inspectionRepository, findingRepository,
            new AuditedFindingGenerator(auditService), clock);
    public final RectifyInspection rectifyInspection = new RectifyInspection(inspectionRepository, clock);
    public final PlanCorrectiveAction planCorrectiveAction = new PlanCorrectiveAction(findingRepository, clock);
    public final VerifyCorrectiveAction verifyCorrectiveAction = new VerifyCorrectiveAction(findingRepository, clock);
    public final GenerateInspectionAct generateInspectionAct = new GenerateInspectionAct(auditService);
    public final GenerateFindingsSummary generateFindingsSummary = new GenerateFindingsSummary(findingRepository, clock);
    public final GenerateCertificateDocument generateCertificateDocument = new GenerateCertificateDocument(clock);

    private final CertificationEligibility certificationEligibility = new NoBlockingFindingsEligibility(findingRepository);
    private final SequentialCertificateNumbering certificateNumbering = new SequentialCertificateNumbering();
    public final IssueCertificate issueCertificate = new IssueCertificate(certificateRepository, certificationEligibility, certificateNumbering,
            new AuditedCertificateGenerator(auditService), clock);
    public final SuspendCertificate suspendCertificate = new SuspendCertificate(certificateRepository, clock);
    public final RenewCertificate renewCertificate = new RenewCertificate(certificateRepository, certificationEligibility, certificateNumbering, clock);

    public final Asset extinguisher = registerAsset.execute(new Asset(new AssetCode("EXT-0042"),
            "Lobby extinguisher", EXTINGUISHER, new Location("Main building, ground floor lobby"), assetResponsible,
            Map.of("capacity", "10 kg", "agent", "ABC powder")));

    public CertiflowFixture() {
        createInspectionSchema.execute("Portable extinguisher inspection", EXTINGUISHER, firstVersionSections());
    }

    public List<Section> firstVersionSections() {
        return List.of(new Section("General condition", List.of(pressureCriterion, sealIntactCriterion, signageCriterion)));
    }

    public Inspection assignedInspection() {
        return assignInspection.execute(extinguisher,
                new InspectionAssignment(inspector, TODAY.plusDays(3), "Annual inspection of the lobby extinguisher"),
                planner);
    }

    public NumericAnswer numericAnswer(String decimalValue) {
        return new NumericAnswer(new BigDecimal(decimalValue));
    }

    public Evidence gaugePhoto() {
        return new Evidence(StandardEvidenceKind.PHOTO, "photos/ext-0042-gauge.jpg", "Pressure gauge reading");
    }

    public void answerAll(Inspection inspection, String pressureInBars, YesNoAnswer sealAnswer, String signageOption) {
        recordAnswer.execute(inspection, pressureCriterion, numericAnswer(pressureInBars), inspector);
        attachEvidence.execute(inspection, pressureCriterion, gaugePhoto(), inspector);
        recordAnswer.execute(inspection, sealIntactCriterion, sealAnswer, inspector);
        recordAnswer.execute(inspection, signageCriterion, new OptionAnswer(signageOption), inspector);
    }

    public Inspection closedCompliantInspection() {
        Inspection inspection = assignedInspection();
        answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");
        closeInspection.execute(inspection, inspector);
        return inspection;
    }

    public Finding majorFindingOf(Inspection inspection) {
        answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        return closeInspection.execute(inspection, inspector).getFirst();
    }
}
