package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.finding.Finding;
import java.util.List;

public interface FindingRepository {

    void save(Finding finding);

    void saveAll(List<Finding> findings);

    List<Finding> findByAsset(Asset asset);
}
