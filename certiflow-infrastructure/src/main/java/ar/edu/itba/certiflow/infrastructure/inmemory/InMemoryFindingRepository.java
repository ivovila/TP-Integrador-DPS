package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryFindingRepository implements FindingRepository {

    private final Map<FindingId, Finding> findings = new LinkedHashMap<>();

    @Override
    public FindingId nextId() {
        return new FindingId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Finding finding) {
        findings.put(finding.id(), finding);
    }

    @Override
    public void saveAll(List<Finding> raisedFindings) {
        raisedFindings.forEach(this::save);
    }

    @Override
    public Optional<Finding> findById(FindingId id) {
        return Optional.ofNullable(findings.get(id));
    }

    @Override
    public List<Finding> findByAsset(AssetCode assetCode) {
        return findings.values().stream().filter(finding -> finding.concerns(assetCode)).toList();
    }
}
