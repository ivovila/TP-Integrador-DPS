package ar.edu.itba.certiflow.shared.usecase;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface AuditHistoryUseCase {

    List<AuditEntry> execute(AuditSubject subject);
}
