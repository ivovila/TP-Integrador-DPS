package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.schema.Section;
import java.time.Clock;
import java.util.List;

public final class PublishSchemaVersion {

    private final InspectionSchemaRepository schemas;
    private final Clock clock;

    public PublishSchemaVersion(InspectionSchemaRepository schemas, Clock clock) {
        this.schemas = schemas;
        this.clock = clock;
    }

    public SchemaVersion execute(AssetType assetType, List<Section> sections) {
        InspectionSchema schema = schemas.findByAssetType(assetType)
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(assetType));
        SchemaVersion version = schema.publish(sections, clock.instant());
        schemas.save(schema);
        return version;
    }
}
