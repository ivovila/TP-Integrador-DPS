package ar.edu.itba.certiflow.usecases.schema;

import ar.edu.itba.certiflow.application.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.time.Clock;
import java.util.List;

public final class PublishSchemaVersion {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final Clock clock;

    public PublishSchemaVersion(InspectionSchemaRepository inspectionSchemaRepository, Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.clock = clock;
    }

    public SchemaVersion execute(AssetType assetType, List<Section> sections) {
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(assetType)
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(assetType));
        SchemaVersion publishedVersion = schema.publish(sections, clock.instant());
        inspectionSchemaRepository.save(schema);
        return publishedVersion;
    }
}
