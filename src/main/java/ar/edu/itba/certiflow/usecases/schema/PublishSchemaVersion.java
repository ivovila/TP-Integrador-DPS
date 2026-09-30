package ar.edu.itba.certiflow.usecases.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.InspectionSchemaService;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.schema.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.time.Clock;
import java.util.List;

public final class PublishSchemaVersion {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public PublishSchemaVersion(InspectionSchemaRepository inspectionSchemaRepository, InspectionSchemaService inspectionSchemaService,
                                Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionSchemaService = inspectionSchemaService;
        this.clock = clock;
    }

    public SchemaVersion execute(AssetType assetType, List<Section> sections, Person publishedBy) {
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(assetType)
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(assetType));
        InspectionSchema updatedSchema = inspectionSchemaService.addVersion(schema, sections, publishedBy, clock.instant());
        inspectionSchemaRepository.save(updatedSchema);
        return updatedSchema.currentVersion();
    }
}
