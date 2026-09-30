package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.ports.FindingRepository;
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
    public List<Finding> findByAsset(Asset asset) {
        return findings.values().stream().filter(finding -> finding.concerns(asset)).toList();
    }
}
