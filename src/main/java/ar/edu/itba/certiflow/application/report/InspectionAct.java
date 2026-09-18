package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.asset.AssetCode;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.domain.inspection.Revision;
import java.util.List;

/**
 * Modelo de lectura sin formato. Las líneas reflejan el acta vigente; las revisiones conservan
 * la original y cada rectificación; el historial es la bitácora de auditoría de la inspección.
 */
public record InspectionAct(AssetCode assetCode, String assetName, InspectionAssignment assignment,
                            int schemaVersion, List<ActLine> lines, List<Revision> revisions,
                            List<AuditEntry> history) {

    public InspectionAct {
        lines = List.copyOf(lines);
        revisions = List.copyOf(revisions);
        history = List.copyOf(history);
    }
}
