package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.Revisions;
import java.util.List;

public record InspectionAct(AssetCode assetCode, String assetName, InspectionAssignment assignment,
                            int schemaVersion, List<ActLine> lines, Revisions revisions,
                            List<AuditEntry> history) {

    public InspectionAct {
        lines = List.copyOf(lines);

        history = List.copyOf(history);
    }
}
