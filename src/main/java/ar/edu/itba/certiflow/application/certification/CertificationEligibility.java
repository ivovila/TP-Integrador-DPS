package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.models.inspection.Inspection;

public interface CertificationEligibility {

    void ensureEligible(Inspection inspection);
}
