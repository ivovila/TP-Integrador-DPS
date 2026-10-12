package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import java.util.List;

public interface InspectionView {

    InspectionId id();

    Evaluation evaluate();

    boolean coversWholeSchema();

    boolean isClosed();

    Revision originalRevision();

    List<Revision> revisions();

    AssetCode assetCode();

    /** El responsable del activo cuando se asignó la inspección: es quien queda a cargo de los hallazgos. */
    Person assetResponsible();

    InspectionAssignment assignment();

    SchemaVersion schemaVersion();
}
