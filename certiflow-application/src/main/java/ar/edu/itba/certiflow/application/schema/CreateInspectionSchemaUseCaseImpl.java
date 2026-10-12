package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import java.time.Clock;
import java.util.List;

public final class CreateInspectionSchemaUseCaseImpl implements CreateInspectionSchemaUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public CreateInspectionSchemaUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository, InspectionSchemaService inspectionSchemaService,
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
