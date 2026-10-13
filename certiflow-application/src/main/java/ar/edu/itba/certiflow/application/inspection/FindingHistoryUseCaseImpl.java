package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.usecase.FindingHistoryUseCase;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class FindingHistoryUseCaseImpl implements FindingHistoryUseCase {

    private final AuditLog<FindingId> findingLog;

    public FindingHistoryUseCaseImpl(AuditLog<FindingId> findingLog) {
        this.findingLog = findingLog;
    }

    @Override
    public List<AuditEntry> execute(FindingId findingId) {
        return findingLog.historyOf(findingId);
    }
}
