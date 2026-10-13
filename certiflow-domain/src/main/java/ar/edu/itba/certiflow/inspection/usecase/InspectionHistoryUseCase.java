package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface InspectionHistoryUseCase {

    List<AuditEntry> execute(InspectionId inspectionId);
}
