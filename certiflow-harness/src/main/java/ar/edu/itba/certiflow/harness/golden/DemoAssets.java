package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.InSubsystem;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.model.Subsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.SchemaSections;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.SchemaVersionLog;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Los dos activos de la demo con la versión 1 de sus esquemas, iguales a los de
 * {@code certiflow-frontend/scripts/seedDemoData.ts}: así el video y el harness hablan del mismo sistema.
 */
public final class DemoAssets {

    /** Un activo y el esquema con que se lo inspecciona. */
    public record DemoAsset(Asset asset, InspectionSchema schema) {
    }

    private static final Instant PUBLISHED = Instant.parse("2026-01-05T09:00:00Z");
    private static final Person RESPONSIBLE = new Person(new PersonId("EMP-003"), "Raul Responsible");
    private static final JurisdictionCode JURISDICTION = new JurisdictionCode("DEMO-STANDARD");
    private static final SubsystemCode PRESSURE_SYSTEM = new SubsystemCode("PRESSURE");
    private static final SubsystemCode ELECTRICAL = new SubsystemCode("ELECTRICAL");

    private final List<DemoAsset> assets = List.of(extinguisher(), pressureVessel());

    public DemoAsset of(String assetType) {
        return assets.stream()
                .filter(demo -> demo.asset().type().name().equals(assetType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The demo has no asset of type " + assetType));
    }

    private DemoAsset extinguisher() {
        AssetType type = new AssetType("Fire extinguisher");
        return new DemoAsset(asset("EXT-0042", "Lobby extinguisher", type, List.of()), schema("Portable extinguisher inspection", type,
                List.of(new Section(new SectionTitle("Physical condition"), List.of(
                                criterion("PRESSURE", "Gauge pressure", range("bar", "10", "16"), StandardSeverity.CRITICAL,
                                        List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1))),
                                criterion("WEIGHT", "Extinguishing agent weight", range("kg", "5.8", "6.2"), StandardSeverity.MAJOR),
                                criterion("SEAL", "Safety seal intact", yes(), StandardSeverity.MAJOR),
                                criterion("HOSE", "Hose condition", options("Good", "Acceptable"), StandardSeverity.MINOR),
                                criterion("CORROSION", "Cylinder free of corrosion", yes(), StandardSeverity.MAJOR))),
                        new Section(new SectionTitle("Installation and records"), List.of(
                                criterion("SIGNAGE", "Signage visible from the corridor", yes(), StandardSeverity.MINOR),
                                criterion("HEIGHT", "Mounting height", range("m", "0.8", "1.5"), StandardSeverity.MINOR),
                                criterion("TAG", "Maintenance tag status", options("Current"), StandardSeverity.MAJOR,
                                        List.of(new EvidenceRequirement(StandardEvidenceKind.DOCUMENT, 1))))))));
    }

    private DemoAsset pressureVessel() {
        AssetType type = new AssetType("Pressure vessel");
        return new DemoAsset(asset("BLR-0007", "Boiler B", type, List.of(new Subsystem(ELECTRICAL, "Electrical installation"),
                new Subsystem(PRESSURE_SYSTEM, "Pressure system"))), schema("Pressure vessel inspection", type, List.of(
                new Section(new SectionTitle("Pressure system"), List.of(
                        criterion("RELIEF-VALVE", "Relief valve opening pressure", range("bar", "7.5", "8.5"), StandardSeverity.CRITICAL),
                        criterion("LEAKS", "No visible leaks", yes(), StandardSeverity.MAJOR),
                        criterion("INSULATION", "Insulation condition", options("Good", "Acceptable"), StandardSeverity.MINOR)),
                        new InSubsystem(PRESSURE_SYSTEM)),
                new Section(new SectionTitle("Electrical installation"), List.of(
                        criterion("GROUNDING", "Burner grounding verified", yes(), StandardSeverity.MAJOR),
                        criterion("PANEL", "Control panel labelled", yes(), StandardSeverity.MINOR)),
                        new InSubsystem(ELECTRICAL)))));
    }

    private Asset asset(String code, String name, AssetType type, List<Subsystem> subsystems) {
        return new Asset(new AssetCode(code), name, type, new Location("Demo site"), RESPONSIBLE, new Characteristics(),
                JURISDICTION, subsystems);
    }

    private InspectionSchema schema(String name, AssetType type, List<Section> sections) {
        return new InspectionSchema(name, type, new SchemaVersionLog(
                List.of(new SchemaVersion(1, new SchemaSections(sections), PUBLISHED, PUBLISHED))));
    }

    private <A extends Answer> Criterion<A> criterion(String code, String text, ApprovalRule<A> rule, Severity severity) {
        return criterion(code, text, rule, severity, List.of());
    }

    private <A extends Answer> Criterion<A> criterion(String code, String text, ApprovalRule<A> rule, Severity severity,
                                                      List<EvidenceRequirement> evidence) {
        return new Criterion<>(new CriterionCode(code), text, rule, severity, evidence);
    }

    private NumericRangeRule range(String unit, String minimum, String maximum) {
        return new NumericRangeRule(new Unit(unit), new Range(new BigDecimal(minimum), new BigDecimal(maximum)));
    }

    private OptionInListRule options(String... accepted) {
        return new OptionInListRule(Set.of(accepted));
    }

    private YesNoRule yes() {
        return new YesNoRule(YesNoAnswer.YES);
    }
}
