package ar.edu.itba.certiflow.schema.model;

import java.util.List;
import java.util.Optional;

/** Los tipos de evidencia que el sistema conoce. Quien lee un código guardado o recibido lo busca acá. */
public final class EvidenceKindCatalog {

    private final List<EvidenceKind> knownEvidenceKinds;

    public EvidenceKindCatalog(List<EvidenceKind> knownEvidenceKinds) {
        this.knownEvidenceKinds = List.copyOf(knownEvidenceKinds);
    }

    public Optional<EvidenceKind> withCode(String code) {
        return knownEvidenceKinds.stream().filter(evidenceKind -> evidenceKind.code().equals(code)).findFirst();
    }

    public List<EvidenceKind> all() {
        return knownEvidenceKinds;
    }
}
