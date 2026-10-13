package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import java.util.List;
import ar.edu.itba.certiflow.catalog.model.AssetType;

public interface InspectionView {

    InspectionId id();

    Evaluation evaluate();

    boolean coversWholeSchema();

    boolean isClosed();

    Revision originalRevision();

    Revisions revisions();

    AssetCode assetCode();

    AssetType assetType();

    /** El responsable del activo cuando se asignó la inspección: es quien queda a cargo de los hallazgos. */
    Person assetResponsible();

    InspectionAssignment assignment();

    SchemaVersion schemaVersion();
}
