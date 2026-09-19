package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import java.util.List;

public interface InspectionView {

    Evaluation evaluate();

    boolean isClosed();

    Revision originalRevision();

    List<Revision> revisions();

    Asset asset();

    InspectionAssignment assignment();

    SchemaVersion schemaVersion();
}
