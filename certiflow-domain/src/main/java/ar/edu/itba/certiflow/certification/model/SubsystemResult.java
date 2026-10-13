package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.List;

public record SubsystemResult(SubsystemCode code, boolean covered, List<OpenNonConformity> openNonConformities) {

    public SubsystemResult {
        openNonConformities = List.copyOf(openNonConformities);
    }
}
