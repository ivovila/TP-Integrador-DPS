package ar.edu.itba.certiflow.inspection.repository;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import java.util.List;
import java.util.Optional;

public interface FindingRepository {

    FindingId nextId();

    void save(Finding finding);

    void saveAll(List<Finding> findings);

    Optional<Finding> findById(FindingId id);

    List<Finding> findByAsset(AssetCode assetCode);
}
