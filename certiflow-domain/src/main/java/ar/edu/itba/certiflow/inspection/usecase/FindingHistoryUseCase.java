package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface FindingHistoryUseCase {

    List<AuditEntry> execute(FindingId findingId);
}
