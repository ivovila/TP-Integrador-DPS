package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import java.util.List;
import java.util.Optional;

public interface FindingRepository {

    FindingId nextId();

    void save(Finding finding);

    void saveAll(List<Finding> findings);

    Optional<Finding> findById(FindingId id);

    List<Finding> findByAsset(Asset asset);
}
