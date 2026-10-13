package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;

/** Un hecho de una inspección que levanta hallazgos: su cierre o una rectificación. */
public interface RaisesFindings {

    InspectionId inspectionId();

    AssetCode assetCode();

    /** Quien queda a cargo de los hallazgos. */
    Person assetResponsible();

    List<CriterionResponse<?>> nonConformities();

    /** Quién y cuándo produjo el hecho: firma el alta de cada hallazgo. */
    Stamp raised();

    SchemaVersion schemaVersion();
}
