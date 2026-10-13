package ar.edu.itba.certiflow.infrastructure.support;

import ar.edu.itba.certiflow.shared.model.Stamp;
import static org.mockito.Mockito.mock;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionClosed;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ar.edu.itba.certiflow.inspection.model.ActionPlan;
import ar.edu.itba.certiflow.shared.model.AuditLog;

public final class DomainSamples {

    public static final AssetType EXTINGUISHER = new AssetType("Fire extinguisher");
    public static final Instant T0 = Instant.parse("2026-03-10T09:00:00Z");
    public static final Person PLANNER = new Person(new PersonId("EMP-001"), "Paula Planner");
    public static final Person INSPECTOR = new Person(new PersonId("EMP-002"), "Ines Inspector");
    public static final Person RESPONSIBLE = new Person(new PersonId("EMP-003"), "Raul Responsible");
    public static final Person TECHNICIAN = new Person(new PersonId("EMP-004"), "Marta Maintenance");
    public static final Person VERIFIER = new Person(new PersonId("EMP-005"), "Victor Verifier");
    public static final Asset ASSET = new Asset(new AssetCode("EXT-0042"), "Lobby extinguisher", EXTINGUISHER, new Location("Lobby"), RESPONSIBLE, Characteristics.none(), new JurisdictionCode("DEMO-STANDARD"), List.of());

    private static final AtomicInteger IDS = new AtomicInteger();

    private DomainSamples() {
    }

    public static InspectionSchema schema() {
        return new InspectionSchema("Extinguisher", EXTINGUISHER, sections(), T0);
    }

    public static List<Section> sections() {
        return List.of(
                new Section("Pressure and seal", List.of(
                        new Criterion<>("EXT-01", "Gauge pressure within range",
                                new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
                                StandardSeverity.MAJOR, List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1))),
                        new Criterion<>("EXT-02", "Seal intact", new YesNoRule(YesNoAnswer.YES), StandardSeverity.CRITICAL,
                                List.of()))),
                new Section("Signage", List.of(new Criterion<>("EXT-03", "Signage condition",
                        new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE")), StandardSeverity.MINOR, List.of()))));
    }

    public static InspectionService inspectionService() {
        return new InspectionService(new InspectionFactory(), mock(AuditLog.class));
    }

    public static Inspection answeredInspection(InspectionSchema schema, String pressure) {
        InspectionService service = inspectionService();
        Inspection inspection = service.assign(new InspectionId("I-" + IDS.incrementAndGet()), ASSET,
                new InspectionAssignment(INSPECTOR, LocalDate.of(2026, 3, 13), new FullScope()), schema.latestPublishedVersion(), new Stamp(PLANNER, T0));
        service.recordAnswer(inspection, "EXT-01", new RawAnswer(new BigDecimal(pressure)), new Stamp(INSPECTOR, T0.plusSeconds(60)));
        service.attachEvidence(inspection, "EXT-01", new Evidence(StandardEvidenceKind.PHOTO, "photos/gauge.jpg", "Gauge"), new Stamp(INSPECTOR, T0.plusSeconds(90)));
        service.addObservation(inspection, "EXT-01", "Needle trembles", new Stamp(INSPECTOR, T0.plusSeconds(100)));
        service.recordAnswer(inspection, "EXT-02", new RawAnswer(true), new Stamp(INSPECTOR, T0.plusSeconds(120)));
        service.recordAnswer(inspection, "EXT-03", new RawAnswer("VISIBLE"), new Stamp(INSPECTOR, T0.plusSeconds(180)));
        return inspection;
    }

    public static InspectionClosed close(Inspection inspection) {
        return inspectionService().close(inspection, new Stamp(INSPECTOR, T0.plusSeconds(600)));
    }

    public static Finding findingWithTwoActions(InspectionClosed closure) {
        FindingService service = new FindingService(new FindingFactory(), mock(AuditLog.class));
        Finding finding = service.raiseFrom(closure, () -> new FindingId("F-" + IDS.incrementAndGet())).getFirst();
        LocalDate today = LocalDate.of(2026, 3, 10);
        service.planAction(finding, new ActionPlan("Recharge", TECHNICIAN, today.plusDays(5)), new Stamp(PLANNER, T0.plusSeconds(700)), today);
        service.planAction(finding, new ActionPlan("Replace", TECHNICIAN, today.plusDays(9)), new Stamp(PLANNER, T0.plusSeconds(710)), today);
        service.verifyAction(finding, finding.actions().getFirst().id(), VerificationResult.REJECTED, "Still low",
                new Stamp(VERIFIER, T0.plusSeconds(800)));
        service.verifyAction(finding, finding.actions().getFirst().id(), VerificationResult.ACCEPTED, "Recharged",
                new Stamp(VERIFIER, T0.plusSeconds(900)));
        return finding;
    }

    public static List<Finding> none() {
        return List.of();
    }
}
