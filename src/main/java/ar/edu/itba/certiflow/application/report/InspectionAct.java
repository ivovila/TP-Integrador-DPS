package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.Revision;
import java.util.List;

public record InspectionAct(AssetCode assetCode, String assetName, InspectionAssignment assignment,
                            int schemaVersion, List<ActLine> lines, List<Revision> revisions,
                            List<AuditEntry> history) {

    public InspectionAct {
        lines = List.copyOf(lines);
        revisions = List.copyOf(revisions);
        history = List.copyOf(history);
    }
}
