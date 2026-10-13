package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.List;

/** Los subsistemas que certifica un alcance: ninguno en particular si es el activo entero. */
final class CertifiedSubsystems implements CertificationScopeDescriber<List<SubsystemCode>> {

    @Override
    public List<SubsystemCode> wholeAsset() {
        return List.of();
    }

    @Override
    public List<SubsystemCode> subsystem(SubsystemCode subsystemCode) {
        return List.of(subsystemCode);
    }
}
