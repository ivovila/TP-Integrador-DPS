package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
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
