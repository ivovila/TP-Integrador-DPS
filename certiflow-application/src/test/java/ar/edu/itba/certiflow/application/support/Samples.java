package ar.edu.itba.certiflow.application.support;

import static org.mockito.Mockito.mock;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
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
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/** Objetos de dominio reales para los tests unitarios de los casos de uso; lo único simulado son los puertos. */
public final class Samples {

    public static final Instant NOW = Instant.parse("2026-03-10T09:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final AssetType EXTINGUISHER = new AssetType("Fire extinguisher");
    public static final Person PLANNER = new Person(new PersonId("EMP-001"), "Paula Planner");
    public static final Person INSPECTOR = new Person(new PersonId("EMP-002"), "Ines Inspector");
    public static final Person RESPONSIBLE = new Person(new PersonId("EMP-003"), "Raul Responsible");
    public static final Asset ASSET = new Asset(new AssetCode("EXT-0042"), "Lobby extinguisher", EXTINGUISHER, new Location("Lobby"),
            RESPONSIBLE, Characteristics.none(), new JurisdictionCode("DEMO-STANDARD"), List.of());

    private Samples() {
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

    public static InspectionSchema schema() {
        return new InspectionSchema("Extinguisher", EXTINGUISHER, sections(), NOW.minusSeconds(3600));
    }

    @SuppressWarnings("unchecked")
    public static <K> AuditLog<K> auditLog() {
        return mock(AuditLog.class);
    }

    public static InspectionService inspectionService() {
        return new InspectionService(new InspectionFactory(), auditLog());
    }

    public static FindingService findingService() {
        return new FindingService(new FindingFactory(), auditLog());
    }

    /** Una inspección con todo respondido; con presión fuera de rango deja una no conformidad mayor. */
    public static Inspection answeredInspection(String pressure) {
        InspectionService service = inspectionService();
        Inspection inspection = service.assign(new InspectionId("I-1"), ASSET,
                new InspectionAssignment(INSPECTOR, LocalDate.of(2026, 3, 13), new FullScope()),
                schema().latestPublishedVersion(), new Stamp(PLANNER, NOW));
        service.recordAnswer(inspection, "EXT-01", new RawAnswer(pressure), new Stamp(INSPECTOR, NOW));
        service.attachEvidence(inspection, "EXT-01", new Evidence(StandardEvidenceKind.PHOTO, "gauge.jpg", "Gauge"), new Stamp(INSPECTOR, NOW));
        service.recordAnswer(inspection, "EXT-02", new RawAnswer("true"), new Stamp(INSPECTOR, NOW));
        service.recordAnswer(inspection, "EXT-03", new RawAnswer("VISIBLE"), new Stamp(INSPECTOR, NOW));
        return inspection;
    }
}
