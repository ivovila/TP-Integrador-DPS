package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import java.util.List;

public interface InspectionRepository {

    void save(Inspection inspection);

    List<Inspection> findByAsset(Asset asset);
}
