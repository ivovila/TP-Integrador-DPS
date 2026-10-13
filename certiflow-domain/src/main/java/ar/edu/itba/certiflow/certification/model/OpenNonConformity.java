package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Severity;
import java.util.Optional;

public record OpenNonConformity(String criterionCode, Severity severity, String findingId, Optional<SubsystemCode> subsystemCode) {

    /** Una no conformidad sin subsistema viene de una inspección sin subsistemas, así que afecta a todos. */
    public boolean affects(SubsystemCode code) {
        return subsystemCode.map(code::equals).orElse(true);
    }

}
