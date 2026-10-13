package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.exceptions.JurisdictionPolicyNotFoundException;
import java.util.Optional;

/**
 * Qué política rige en una jurisdicción. Dar de alta un activo necesita preguntar si hay una configurada,
 * para rechazar el alta como dato inválido; certificar necesita la política o no puede seguir. {@code resolve}
 * es esa segunda forma de la misma pregunta, así que nadie tiene que atrapar una excepción para decidir.
 */
public interface CertificationPolicyResolver {

    Optional<CertificationPolicy> find(JurisdictionCode jurisdictionCode);

    default CertificationPolicy resolve(JurisdictionCode jurisdictionCode) {
        return find(jurisdictionCode).orElseThrow(() -> new JurisdictionPolicyNotFoundException(jurisdictionCode));
    }
}
