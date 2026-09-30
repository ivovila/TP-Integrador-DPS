package ar.edu.itba.certiflow.usecases.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.InspectionSchemaService;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.schema.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.time.Clock;
import java.util.List;

public final class CreateInspectionSchema {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public CreateInspectionSchema(InspectionSchemaRepository inspectionSchemaRepository, InspectionSchemaService inspectionSchemaService,
                                  Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionSchemaService = inspectionSchemaService;
        this.clock = clock;
    }

    public InspectionSchema execute(String name, AssetType assetType, List<Section> sections, Person createdBy) {
        inspectionSchemaRepository.findByAssetType(assetType).ifPresent(existingSchema -> {
            throw new SchemaAlreadyDefinedForAssetTypeException(assetType);
        });
        InspectionSchema schema = inspectionSchemaService.create(name, assetType, sections, createdBy, clock.instant());
        inspectionSchemaRepository.save(schema);
        return schema;
    }
}
