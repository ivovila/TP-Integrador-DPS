package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import java.time.Instant;
import java.time.LocalDate;

public record CertificationAssessmentContext(InspectionResult result, JurisdictionCode jurisdictionCode,
                                             Instant evaluatedAt, LocalDate issuanceDate) {
}
