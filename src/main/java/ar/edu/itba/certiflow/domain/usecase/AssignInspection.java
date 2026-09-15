package ar.edu.itba.certiflow.domain.usecase;

import java.time.LocalDate;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.ports.AssetRepository;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;
import ar.edu.itba.certiflow.domain.ports.SchemaRepository;

public class AssignInspection {

    private final AssetRepository assets;
    private final SchemaRepository schemas;
    private final InspectionRepository inspections;

    public AssignInspection(AssetRepository assets, SchemaRepository schemas, InspectionRepository inspections) {
        this.assets = assets;
        this.schemas = schemas;
        this.inspections = inspections;
    }

    public Inspection execute(AssetId assetId, SchemaId schemaId, PersonId inspector, LocalDate scheduledDate,
                              String scope) {
        Asset asset = assets.findById(assetId)
                .orElseThrow(() -> new NotFoundException("el activo", assetId.value()));
        InspectionSchema schema = schemas.findById(schemaId)
                .orElseThrow(() -> new NotFoundException("el esquema", schemaId.value()));
        Inspection inspection = Inspection.assign(InspectionId.generate(), asset, schema, inspector,
                scheduledDate, scope);
        inspections.save(inspection);
        return inspection;
    }
}
