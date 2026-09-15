package ar.edu.itba.certiflow.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;

public class InMemoryFindingRepository implements FindingRepository {

    private final Map<FindingId, Finding> findings = new LinkedHashMap<>();

    @Override
    public Optional<Finding> findById(FindingId id) {
        return Optional.ofNullable(findings.get(id));
    }

    @Override
    public void save(Finding finding) {
        findings.put(finding.getId(), finding);
    }

    @Override
    public List<Finding> findByInspection(InspectionId inspectionId) {
        return findings.values().stream()
                .filter(finding -> finding.getInspection().equals(inspectionId))
                .toList();
    }

    @Override
    public List<Finding> findByAsset(AssetId assetId) {
        return findings.values().stream()
                .filter(finding -> finding.getAsset().equals(assetId))
                .toList();
    }
}
