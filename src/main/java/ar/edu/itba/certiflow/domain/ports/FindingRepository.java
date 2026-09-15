package ar.edu.itba.certiflow.domain.ports;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;

public interface FindingRepository {

    Optional<Finding> findById(FindingId id);

    default Finding getById(FindingId id) {
        return findById(id).orElseThrow(() -> new NotFoundException("el hallazgo", id.value()));
    }

    void save(Finding finding);

    List<Finding> findByInspection(InspectionId inspectionId);

    List<Finding> findByAsset(AssetId assetId);
}
