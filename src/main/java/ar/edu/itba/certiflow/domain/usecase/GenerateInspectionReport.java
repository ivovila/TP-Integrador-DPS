package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.report.InspectionReport;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class GenerateInspectionReport {

    private final InspectionRepository inspections;

    public GenerateInspectionReport(InspectionRepository inspections) {
        this.inspections = inspections;
    }

    public InspectionReport execute(InspectionId inspectionId) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        return InspectionReport.of(inspection);
    }
}
