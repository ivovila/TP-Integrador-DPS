package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.schema.Section;
import java.time.Clock;
import java.util.List;

public final class CreateInspectionSchema {

    private final InspectionSchemaRepository schemas;
    private final Clock clock;

    public CreateInspectionSchema(InspectionSchemaRepository schemas, Clock clock) {
        this.schemas = schemas;
        this.clock = clock;
    }

    public InspectionSchema execute(String name, AssetType assetType, List<Section> sections) {
        schemas.findByAssetType(assetType).ifPresent(existing -> {
            throw new SchemaAlreadyDefinedForAssetTypeException(assetType);
        });
        InspectionSchema schema = new InspectionSchema(name, assetType, sections, clock.instant());
        schemas.save(schema);
        return schema;
    }
}
