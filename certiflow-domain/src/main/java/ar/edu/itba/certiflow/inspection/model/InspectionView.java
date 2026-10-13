package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.Optional;

public interface InspectionView {

    InspectionId id();

    Evaluation evaluate();

    boolean coversWholeSchema();

    boolean isClosed();

    /** Cuándo se cerró la inspección, si ya se cerró. */
    Optional<Instant> closedAt();

    Revision originalRevision();

    Revisions revisions();

    AssetCode assetCode();

    AssetType assetType();

    /** El responsable del activo cuando se asignó la inspección: es quien queda a cargo de los hallazgos. */
    Person assetResponsible();

    InspectionAssignment assignment();

    SchemaVersion schemaVersion();
}
