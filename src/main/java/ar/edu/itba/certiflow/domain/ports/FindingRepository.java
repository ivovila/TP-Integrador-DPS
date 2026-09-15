package ar.edu.itba.certiflow.domain.ports;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;

public interface FindingRepository {

    Optional<Finding> findById(FindingId id);

    void save(Finding finding);

    List<Finding> findByInspection(InspectionId inspectionId);

    List<Finding> findByAsset(AssetId assetId);
}
