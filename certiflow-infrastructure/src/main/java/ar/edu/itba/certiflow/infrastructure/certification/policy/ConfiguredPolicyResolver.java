package ar.edu.itba.certiflow.infrastructure.certification.policy;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.*;
import ar.edu.itba.certiflow.certification.model.exceptions.JurisdictionPolicyNotFoundException;
import java.util.Map;

public final class ConfiguredPolicyResolver implements CertificationPolicyResolver {
    private final Map<JurisdictionCode, CertificationPolicy> policies;

    public ConfiguredPolicyResolver(Map<JurisdictionCode, CertificationPolicy> policies) {
        this.policies = Map.copyOf(policies);
        if (policies.isEmpty()) { throw new IllegalArgumentException("At least one jurisdiction policy is required"); }
    }

    @Override
    public CertificationPolicy resolve(JurisdictionCode code) {
        var policy = policies.get(code);
        if (policy == null) { throw new JurisdictionPolicyNotFoundException(code); }
        return policy;
    }
}
