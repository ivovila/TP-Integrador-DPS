package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
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
        Inspection inspection = inspections.getById(inspectionId);
        inspection.close(clock.now());
        inspections.save(inspection);
        events.publishFrom(inspection);
        return inspection.getEvaluation();
    }
}
