package ar.edu.itba.certiflow.usecases.schema;

import ar.edu.itba.certiflow.application.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.time.Clock;
import java.util.List;

public final class CreateInspectionSchema {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final Clock clock;

    public CreateInspectionSchema(InspectionSchemaRepository inspectionSchemaRepository, Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.clock = clock;
    }

    public InspectionSchema execute(String name, AssetType assetType, List<Section> sections) {
        inspectionSchemaRepository.findByAssetType(assetType).ifPresent(existingSchema -> {
            throw new SchemaAlreadyDefinedForAssetTypeException(assetType);
        });
        InspectionSchema schema = new InspectionSchema(name, assetType, sections, clock.instant());
        inspectionSchemaRepository.save(schema);
        return schema;
    }
}
