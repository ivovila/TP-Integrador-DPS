package ar.edu.itba.certiflow.domain.usecase;

import java.util.Map;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.inspection.Rectification;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class RectifyInspection {

    private final InspectionRepository inspections;
    private final Clock clock;
    private final EventPublisher events;

    public RectifyInspection(InspectionRepository inspections, Clock clock, EventPublisher events) {
        this.inspections = inspections;
        this.clock = clock;
        this.events = events;
    }

    public InspectionEvaluation execute(InspectionId inspectionId, PersonId author, String reason,
                                        Map<CriterionId, Response> corrections) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        inspection.rectify(new Rectification(author, reason, clock.now(), corrections));
        inspections.save(inspection);
        inspection.pullEvents().forEach(events::publish);
        return inspection.evaluate();
    }
}
