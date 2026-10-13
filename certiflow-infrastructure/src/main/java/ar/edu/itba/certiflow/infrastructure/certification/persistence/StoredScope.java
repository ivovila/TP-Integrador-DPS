package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.SubsystemScope;
import ar.edu.itba.certiflow.certification.model.WholeAssetScope;
import java.util.Optional;

/** El alcance como está guardado: el código del subsistema, o nada si es el activo entero. */
record StoredScope(String subsystemCode) {

    CertificationScope scope() {
        return Optional.ofNullable(subsystemCode)
                .<CertificationScope>map(storedSubsystemCode -> new SubsystemScope(new SubsystemCode(storedSubsystemCode)))
                .orElseGet(WholeAssetScope::new);
    }
}
