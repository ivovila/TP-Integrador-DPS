package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

/** El harness arma inspecciones de casos inventados: su bitácora no le importa a nadie y no se guarda. */
public final class UnrecordedAuditLog<S> implements AuditLog<S> {

    @Override
    public void record(S subject, AuditEntry entry) {
        // Intencionalmente vacío: ver el comentario de la clase.
    }

    @Override
    public List<AuditEntry> historyOf(S subject) {
        return List.of();
    }
}
