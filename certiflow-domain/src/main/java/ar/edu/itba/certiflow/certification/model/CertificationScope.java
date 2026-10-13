package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.Objects;

public sealed interface CertificationScope permits CertificationScope.WholeAsset, CertificationScope.SubsystemScope {
    record WholeAsset(String kind) implements CertificationScope {
        public WholeAsset() { this("WHOLE_ASSET"); }
        public WholeAsset { if (!"WHOLE_ASSET".equals(kind)) { throw new IllegalArgumentException("Invalid whole asset scope"); } }
    }
    record SubsystemScope(SubsystemCode code) implements CertificationScope {
        public SubsystemScope { Objects.requireNonNull(code); }
    }
}
