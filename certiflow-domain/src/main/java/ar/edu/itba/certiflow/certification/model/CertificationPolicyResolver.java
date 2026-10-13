package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;

public interface CertificationPolicyResolver {
    CertificationPolicy resolve(JurisdictionCode jurisdictionCode);
}
