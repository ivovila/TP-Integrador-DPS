package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SectionsDocumentMapper;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingSnapshot;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaFindingRepository implements FindingRepository {

    private final SpringDataFindingRepository findings;
    private final FindingFactory factory = new FindingFactory();

    private final SectionsDocumentMapper sectionsDocuments;
    private final EvidenceKindCatalog evidenceKinds;

    public JpaFindingRepository(SpringDataFindingRepository findings, SectionsDocumentMapper sectionsDocuments,
                                EvidenceKindCatalog evidenceKinds) {
        this.findings = findings;
        this.sectionsDocuments = sectionsDocuments;
        this.evidenceKinds = evidenceKinds;
    }

    @Override
    public FindingId nextId() {
        return new FindingId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Finding finding) {
        FindingSnapshot snapshot = finding.snapshot();
        findings.findById(snapshot.id().value()).ifPresentOrElse(
                existing -> existing.copyActionsFrom(snapshot),
                () -> findings.save(new FindingEntity(snapshot, sectionsDocuments)));
    }

    @Override
    public void saveAll(List<Finding> raisedFindings) {
        raisedFindings.forEach(this::save);
    }

    @Override
    public Optional<Finding> findById(FindingId id) {
        return findings.findById(id.value()).map(this::restore);
    }

    @Override
    public List<Finding> findByAsset(AssetCode assetCode) {
        return findings.findByAssetCode(assetCode.value()).stream().map(this::restore).toList();
    }

    @Override
    public List<Finding> findAll() {
        return findings.findAll().stream().map(this::restore).toList();
    }

    private Finding restore(FindingEntity entity) {
        return factory.restore(entity.toSnapshot(sectionsDocuments, evidenceKinds));
    }
}
