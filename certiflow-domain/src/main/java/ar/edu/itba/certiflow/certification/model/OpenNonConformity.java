package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.schema.model.Severity;

public record OpenNonConformity(String criterionCode, Severity severity) {

    public boolean blocksCertification() {
        return severity.blocksCertification();
    }
}
