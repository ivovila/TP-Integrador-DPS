package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.InspectionSchemaService;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.schema.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import ar.edu.itba.certiflow.usecases.schema.CreateInspectionSchema;
import java.time.Clock;
import java.util.List;

public final class CreateInspectionSchemaInteractor implements CreateInspectionSchema {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public CreateInspectionSchemaInteractor(InspectionSchemaRepository inspectionSchemaRepository, InspectionSchemaService inspectionSchemaService,
                                            Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionSchemaService = inspectionSchemaService;
        this.clock = clock;
    }

    @Override
    public InspectionSchema execute(String name, AssetType assetType, List<Section> sections, Person createdBy) {
        inspectionSchemaRepository.findByAssetType(assetType).ifPresent(existingSchema -> {
            throw new SchemaAlreadyDefinedForAssetTypeException(assetType);
        });
        InspectionSchema schema = inspectionSchemaService.create(name, assetType, sections, createdBy, clock.instant());
        inspectionSchemaRepository.save(schema);
        return schema;
    }
}
