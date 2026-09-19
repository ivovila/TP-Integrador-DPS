package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.finding.Finding;
import java.util.List;

public interface FindingRepository {

    void save(Finding finding);

    void saveAll(List<Finding> findings);

    List<Finding> findByAsset(Asset asset);
}
