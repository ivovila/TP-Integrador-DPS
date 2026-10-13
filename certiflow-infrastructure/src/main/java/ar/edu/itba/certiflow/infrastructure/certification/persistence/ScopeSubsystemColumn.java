package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScopeDescriber;
import java.util.Optional;

final class ScopeSubsystemColumn implements CertificationScopeDescriber<Optional<String>> {

    @Override
    public Optional<String> wholeAsset() {
        return Optional.empty();
    }

    @Override
    public Optional<String> subsystem(SubsystemCode subsystemCode) {
        return Optional.of(subsystemCode.value());
    }
}
