package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SchemaVersionEntity;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.StoredSchemaVersions;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionSnapshot;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionRepository implements InspectionRepository {

    private final SpringDataInspectionRepository inspections;
    private final StoredSchemaVersions schemaVersions;
    private final CodesDocumentMapper codeDocuments;
    private final EvaluationDocumentMapper evaluationDocuments;
    private final EvidenceKindCatalog evidenceKinds;
    private final InspectionFactory factory = new InspectionFactory();

    public JpaInspectionRepository(SpringDataInspectionRepository inspections, StoredSchemaVersions schemaVersions,
                                   CodesDocumentMapper codeDocuments, EvaluationDocumentMapper evaluationDocuments,
                                   EvidenceKindCatalog evidenceKinds) {
        this.inspections = inspections;
        this.schemaVersions = schemaVersions;
        this.codeDocuments = codeDocuments;
        this.evaluationDocuments = evaluationDocuments;
        this.evidenceKinds = evidenceKinds;
    }

    @Override
    public InspectionId nextId() {
        return new InspectionId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Inspection inspection) {
        InspectionSnapshot snapshot = inspection.snapshot();
        inspections.findById(snapshot.id().value()).ifPresentOrElse(
                existing -> existing.copyFrom(snapshot, codeDocuments, evaluationDocuments),
                () -> inspections.save(newEntityOf(snapshot)));
    }

    @Override
    public Optional<Inspection> findById(InspectionId id) {
        return inspections.findById(id.value()).map(this::restore);
    }

    @Override
    public List<Inspection> findByAsset(AssetCode assetCode) {
        return inspections.findByAssetCode(assetCode.value()).stream().map(this::restore).toList();
    }

    @Override
    public List<Inspection> findAll() {
        return inspections.findAll().stream().map(this::restore).toList();
    }

    private InspectionEntity newEntityOf(InspectionSnapshot snapshot) {
        SchemaVersionEntity schemaVersion = schemaVersions.entityOf(snapshot.assetType(),
                snapshot.schemaVersion().number());
        return new InspectionEntity(snapshot, schemaVersion, codeDocuments, evaluationDocuments);
    }

    private Inspection restore(InspectionEntity entity) {
        return factory.restore(entity.toSnapshot(schemaVersions, codeDocuments, evaluationDocuments, evidenceKinds));
    }
}
