package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.inspection.Inspection;

public interface CertificationEligibility {

    void ensureEligible(Inspection inspection);
}
