package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.schema.Section;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class CreateInspectionSchema {

    private final InspectionSchemaRepository schemas;
    private final Clock clock;

    public CreateInspectionSchema(InspectionSchemaRepository schemas, Clock clock) {
        this.schemas = Objects.requireNonNull(schemas, "schemas");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Cada tipo de activo está sujeto a un único esquema; sus cambios se publican como versiones. */
    public InspectionSchema execute(String name, AssetType assetType, List<Section> sections) {
        schemas.findByAssetType(assetType).ifPresent(existing -> {
            throw new SchemaAlreadyDefinedForAssetTypeException(assetType);
        });
        InspectionSchema schema = new InspectionSchema(name, assetType, sections, clock.instant());
        schemas.save(schema);
        return schema;
    }
}
