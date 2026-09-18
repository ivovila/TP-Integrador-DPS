package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.application.finding.FindingRepository;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.finding.Finding;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InMemoryFindingRepository implements FindingRepository {

    private final Set<Finding> findings = new LinkedHashSet<>();

    @Override
    public void save(Finding finding) {
        findings.add(finding);
    }

    @Override
    public void saveAll(List<Finding> raised) {
        findings.addAll(raised);
    }

    @Override
    public List<Finding> findByAsset(Asset asset) {
        return findings.stream().filter(finding -> finding.concerns(asset)).toList();
    }
}
