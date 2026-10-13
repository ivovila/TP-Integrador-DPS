package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Severity;

public record OpenNonConformity(String criterionCode, Severity severity, String findingId, SubsystemAssociation subsystem) {


    /** Una no conformidad sin subsistema viene de una inspección sin subsistemas, así que afecta a todos. */
    public boolean affects(SubsystemCode code) {
        return subsystem.reaches(code);
    }

}
