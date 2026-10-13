package ar.edu.itba.certiflow.shared.model;

import java.util.List;
import java.util.Optional;

/** Las acciones de bitácora que el sistema conoce. Quien lee un código guardado lo busca acá. */
public final class AuditActionCatalog {

    private final List<AuditAction> knownActions;

    public AuditActionCatalog(List<AuditAction> knownActions) {
        this.knownActions = List.copyOf(knownActions);
    }

    public Optional<AuditAction> withCode(String code) {
        return knownActions.stream().filter(action -> action.code().equals(code)).findFirst();
    }

    public List<AuditAction> all() {
        return knownActions;
    }
}
