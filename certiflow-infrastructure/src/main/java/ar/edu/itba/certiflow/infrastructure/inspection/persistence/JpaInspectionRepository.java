package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionSnapshot;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionRepository implements InspectionRepository {

    private final SpringDataInspectionRepository inspections;
    private final JpaInspectionSchemaRepository schemas;
    private final InspectionFactory factory = new InspectionFactory();

    public JpaInspectionRepository(SpringDataInspectionRepository inspections, JpaInspectionSchemaRepository schemas) {
        this.inspections = inspections;
        this.schemas = schemas;
    }

    @Override
    public InspectionId nextId() {
        return new InspectionId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Inspection inspection) {
        InspectionSnapshot snapshot = inspection.snapshot();
        inspections.findById(snapshot.id().value()).ifPresentOrElse(
                existing -> existing.copyFrom(snapshot),
                () -> inspections.save(new InspectionEntity(snapshot, schemas.versionEntityFor(snapshot.schemaVersion()))));
    }

    @Override
    public Optional<Inspection> findById(InspectionId id) {
        return inspections.findById(id.value()).map(entity -> factory.restore(entity.toSnapshot()));
    }

    @Override
    public List<Inspection> findByAsset(AssetCode assetCode) {
        return inspections.findByAssetCode(assetCode.value()).stream().map(entity -> factory.restore(entity.toSnapshot())).toList();
    }

    @Override
    public List<Inspection> findAll() {
        return inspections.findAll().stream().map(entity -> factory.restore(entity.toSnapshot())).toList();
    }
}
