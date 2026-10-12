package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.Revisions;
import java.util.List;

public record InspectionAct(AssetCode assetCode, String assetName, InspectionAssignment assignment,
                            int schemaVersion, List<ActLine> lines, Revisions revisions,
                            List<AuditEntry> history) {

    public InspectionAct {
        lines = List.copyOf(lines);

        history = List.copyOf(history);
    }
}
