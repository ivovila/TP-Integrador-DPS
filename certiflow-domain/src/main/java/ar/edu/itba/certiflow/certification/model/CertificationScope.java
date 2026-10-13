package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.Objects;
import java.util.Optional;

public sealed interface CertificationScope permits CertificationScope.WholeAsset, CertificationScope.SubsystemScope {

    /** El subsistema que certifica, si el alcance es un subsistema. */
    Optional<SubsystemCode> subsystemCode();

    record WholeAsset(String kind) implements CertificationScope {
        public WholeAsset() { this("WHOLE_ASSET"); }
        public WholeAsset { if (!"WHOLE_ASSET".equals(kind)) { throw new IllegalArgumentException("Invalid whole asset scope"); } }

        @Override
        public Optional<SubsystemCode> subsystemCode() { return Optional.empty(); }
    }
    record SubsystemScope(SubsystemCode code) implements CertificationScope {
        public SubsystemScope { Objects.requireNonNull(code); }

        @Override
        public Optional<SubsystemCode> subsystemCode() { return Optional.of(code); }
    }
}
