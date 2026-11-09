package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.catalog.model.Subsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.SchemaSections;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.SchemaVersionLog;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.time.Instant;
import java.util.List;

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
                new DemoSchemas().of(type.name())));
    }

    private DemoAsset pressureVessel() {
        AssetType type = new AssetType("Pressure vessel");
        return new DemoAsset(asset("BLR-0007", "Boiler B", type, List.of(new Subsystem(ELECTRICAL, "Electrical installation"),
                new Subsystem(PRESSURE_SYSTEM, "Pressure system"))), schema("Pressure vessel inspection", type,
                new DemoSchemas().of(type.name())));
    }

    private Asset asset(String code, String name, AssetType type, List<Subsystem> subsystems) {
        return new Asset(new AssetCode(code), name, type, new Location("Demo site"), RESPONSIBLE, new Characteristics(),
                JURISDICTION, subsystems);
    }

    private InspectionSchema schema(String name, AssetType type, List<Section> sections) {
        return new InspectionSchema(name, type, new SchemaVersionLog(
                List.of(new SchemaVersion(1, new SchemaSections(sections), PUBLISHED, PUBLISHED))));
    }

}
