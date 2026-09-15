package ar.edu.itba.certiflow.domain.model.certificate;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;

public interface CertificationPolicy {

    boolean allows(InspectionEvaluation evaluation, List<Finding> findings);
}
