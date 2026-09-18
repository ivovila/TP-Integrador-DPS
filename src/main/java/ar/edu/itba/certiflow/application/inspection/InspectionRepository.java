package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import java.util.List;

public interface InspectionRepository {

    void save(Inspection inspection);

    List<Inspection> findByAsset(Asset asset);
}
