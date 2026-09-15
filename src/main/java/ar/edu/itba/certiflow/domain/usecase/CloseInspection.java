package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class CloseInspection {

    private final InspectionRepository inspections;
    private final Clock clock;
    private final EventPublisher events;

    public CloseInspection(InspectionRepository inspections, Clock clock, EventPublisher events) {
        this.inspections = inspections;
        this.clock = clock;
        this.events = events;
    }

    public InspectionEvaluation execute(InspectionId inspectionId) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        inspection.close(clock.now());
        inspections.save(inspection);
        inspection.pullEvents().forEach(events::publish);
        return inspection.getEvaluation();
    }
}
