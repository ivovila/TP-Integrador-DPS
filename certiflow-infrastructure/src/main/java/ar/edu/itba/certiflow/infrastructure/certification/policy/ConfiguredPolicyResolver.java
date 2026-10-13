package ar.edu.itba.certiflow.infrastructure.certification.policy;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.CertificationPolicy;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.JurisdictionPolicy;
import ar.edu.itba.certiflow.certification.model.RegisteredJurisdictions;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registro inmutable por proceso: la composición es el único lugar que asocia jurisdicción con política. */
public final class ConfiguredPolicyResolver implements CertificationPolicyResolver, RegisteredJurisdictions {

    private final Map<JurisdictionCode, CertificationPolicy> policies;

    public ConfiguredPolicyResolver(Map<JurisdictionCode, CertificationPolicy> policies) {
        this.policies = Map.copyOf(policies);
        if (policies.isEmpty()) {
            throw new IllegalArgumentException("At least one jurisdiction policy is required");
        }
    }

    @Override
    public Optional<CertificationPolicy> find(JurisdictionCode jurisdictionCode) {
        return Optional.ofNullable(policies.get(jurisdictionCode));
    }

    @Override
    public List<JurisdictionPolicy> all() {
        return policies.entrySet().stream()
                .map(registered -> new JurisdictionPolicy(registered.getKey(), registered.getValue().code()))
                .sorted(Comparator.comparing(registered -> registered.jurisdictionCode().value()))
                .toList();
    }
}
