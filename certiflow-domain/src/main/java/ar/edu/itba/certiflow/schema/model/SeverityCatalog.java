package ar.edu.itba.certiflow.schema.model;

import java.util.List;
import java.util.Optional;

/** Las severidades que el sistema conoce. Quien lee un código guardado o recibido lo busca acá. */
public final class SeverityCatalog {

    private final List<Severity> knownSeverities;

    public SeverityCatalog(List<Severity> knownSeverities) {
        this.knownSeverities = List.copyOf(knownSeverities);
    }

    public Optional<Severity> withCode(String code) {
        return knownSeverities.stream().filter(severity -> severity.code().equals(code)).findFirst();
    }

    public List<Severity> all() {
        return knownSeverities;
    }
}
