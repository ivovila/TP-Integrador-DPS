package ar.edu.itba.certiflow.support;

import ar.edu.itba.certiflow.application.asset.RegisterAsset;
import ar.edu.itba.certiflow.application.certificate.CertificationEligibility;
import ar.edu.itba.certiflow.application.certificate.IssueCertificate;
import ar.edu.itba.certiflow.application.certificate.RenewCertificate;
import ar.edu.itba.certiflow.application.certificate.SuspendCertificate;
import ar.edu.itba.certiflow.application.finding.PlanCorrectiveAction;
import ar.edu.itba.certiflow.application.finding.VerifyCorrectiveAction;
import ar.edu.itba.certiflow.application.inspection.AddObservation;
import ar.edu.itba.certiflow.application.inspection.AssignInspection;
import ar.edu.itba.certiflow.application.inspection.AttachEvidence;
import ar.edu.itba.certiflow.application.inspection.CloseInspection;
import ar.edu.itba.certiflow.application.inspection.RecordAnswer;
import ar.edu.itba.certiflow.application.inspection.RectifyInspection;
import ar.edu.itba.certiflow.application.report.GenerateCertificateDocument;
import ar.edu.itba.certiflow.application.report.GenerateFindingsSummary;
import ar.edu.itba.certiflow.application.report.GenerateInspectionAct;
import ar.edu.itba.certiflow.application.schema.CreateInspectionSchema;
import ar.edu.itba.certiflow.application.schema.PublishSchemaVersion;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.asset.AssetCode;
import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.asset.Location;
import ar.edu.itba.certiflow.domain.certificate.AuditedCertificateGenerator;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.EvidenceRequirement;
import ar.edu.itba.certiflow.domain.evaluation.NumericAnswer;
import ar.edu.itba.certiflow.domain.evaluation.NumericRangeRule;
import ar.edu.itba.certiflow.domain.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.domain.evaluation.OptionInListRule;
import ar.edu.itba.certiflow.domain.evaluation.Range;
import ar.edu.itba.certiflow.domain.evaluation.StandardEvidenceKind;
import ar.edu.itba.certiflow.domain.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.domain.evaluation.Unit;
import ar.edu.itba.certiflow.domain.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.domain.evaluation.YesNoRule;
import ar.edu.itba.certiflow.domain.finding.AuditedFindingGenerator;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.inspection.AuditedInspectionGenerator;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.schema.Section;
import ar.edu.itba.certiflow.domain.shared.Person;
import ar.edu.itba.certiflow.details.inmemory.InMemoryAssetRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryAuditService;
import ar.edu.itba.certiflow.details.inmemory.InMemoryCertificateRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryFindingRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryInspectionRepository;
import ar.edu.itba.certiflow.details.inmemory.InMemoryInspectionSchemaRepository;
import ar.edu.itba.certiflow.details.inmemory.SequentialCertificateNumbering;
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
    public final Person maintenance = new Person("Marta Maintenance");
    public final Person verifier = new Person("Victor Verifier");
    public final Person certifier = new Person("Carla Certifier");

    public final Criterion<NumericAnswer> pressure = new Criterion<>("EXT-01", "Gauge pressure within range",
            new NumericRangeRule(BAR, new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
            StandardSeverity.MAJOR, List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1)));
    public final Criterion<YesNoAnswer> sealIntact = new Criterion<>("EXT-02", "Safety seal is intact",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.CRITICAL, List.of());
    public final Criterion<OptionAnswer> signage = new Criterion<>("EXT-03", "Signage condition",
            new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE")), StandardSeverity.MINOR, List.of());

    public final MutableClock clock = new MutableClock(Instant.parse("2026-03-10T09:00:00Z"));
    public final InMemoryAuditService auditService = new InMemoryAuditService();
    public final InMemoryAssetRepository assets = new InMemoryAssetRepository();
    public final InMemoryInspectionSchemaRepository schemas = new InMemoryInspectionSchemaRepository();
    public final InMemoryInspectionRepository inspections = new InMemoryInspectionRepository();
    public final InMemoryFindingRepository findings = new InMemoryFindingRepository();
    public final InMemoryCertificateRepository certificates = new InMemoryCertificateRepository();

    public final RegisterAsset registerAsset = new RegisterAsset(assets);
    public final CreateInspectionSchema createInspectionSchema = new CreateInspectionSchema(schemas, clock);
    public final PublishSchemaVersion publishSchemaVersion = new PublishSchemaVersion(schemas, clock);
    public final AssignInspection assignInspection = new AssignInspection(schemas, inspections,
            new AuditedInspectionGenerator(auditService), clock);
    public final RecordAnswer recordAnswer = new RecordAnswer(inspections, clock);
    public final AttachEvidence attachEvidence = new AttachEvidence(inspections, clock);
    public final AddObservation addObservation = new AddObservation(inspections, clock);
    public final CloseInspection closeInspection = new CloseInspection(inspections, findings,
            new AuditedFindingGenerator(auditService), clock);
    public final RectifyInspection rectifyInspection = new RectifyInspection(inspections, clock);
    public final PlanCorrectiveAction planCorrectiveAction = new PlanCorrectiveAction(findings, clock);
    public final VerifyCorrectiveAction verifyCorrectiveAction = new VerifyCorrectiveAction(findings, clock);
    public final GenerateInspectionAct generateInspectionAct = new GenerateInspectionAct(auditService);
    public final GenerateFindingsSummary generateFindingsSummary = new GenerateFindingsSummary(findings, clock);
    public final GenerateCertificateDocument generateCertificateDocument = new GenerateCertificateDocument(clock);

    private final CertificationEligibility eligibility = new CertificationEligibility(findings);
    private final SequentialCertificateNumbering numbering = new SequentialCertificateNumbering();
    public final IssueCertificate issueCertificate = new IssueCertificate(certificates, eligibility, numbering,
            new AuditedCertificateGenerator(auditService), clock);
    public final SuspendCertificate suspendCertificate = new SuspendCertificate(certificates, clock);
    public final RenewCertificate renewCertificate = new RenewCertificate(certificates, eligibility, numbering, clock);

    public final Asset extinguisher = registerAsset.execute(new Asset(new AssetCode("EXT-0042"),
            "Lobby extinguisher", EXTINGUISHER, new Location("Main building, ground floor lobby"), assetResponsible,
            Map.of("capacity", "10 kg", "agent", "ABC powder")));

    public CertiflowFixture() {
        createInspectionSchema.execute("Portable extinguisher inspection", EXTINGUISHER, firstVersionSections());
    }

    public List<Section> firstVersionSections() {
        return List.of(new Section("General condition", List.of(pressure, sealIntact, signage)));
    }

    public Inspection assignedInspection() {
        return assignInspection.execute(extinguisher,
                new InspectionAssignment(inspector, TODAY.plusDays(3), "Annual inspection of the lobby extinguisher"),
                planner);
    }

    public NumericAnswer numericAnswer(String value) {
        return new NumericAnswer(new BigDecimal(value));
    }

    public Evidence gaugePhoto() {
        return new Evidence(StandardEvidenceKind.PHOTO, "photos/ext-0042-gauge.jpg", "Pressure gauge reading");
    }

    public void answerAll(Inspection inspection, String pressureInBars, YesNoAnswer seal, String signageOption) {
        recordAnswer.execute(inspection, pressure, numericAnswer(pressureInBars), inspector);
        attachEvidence.execute(inspection, pressure, gaugePhoto(), inspector);
        recordAnswer.execute(inspection, sealIntact, seal, inspector);
        recordAnswer.execute(inspection, signage, new OptionAnswer(signageOption), inspector);
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
