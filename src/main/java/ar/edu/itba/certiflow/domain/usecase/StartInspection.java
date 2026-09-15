package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;
import ar.edu.itba.certiflow.domain.ports.SchemaRepository;

public class StartInspection {

    private final InspectionRepository inspections;
    private final SchemaRepository schemas;
    private final Clock clock;
    private final EventPublisher events;

    public StartInspection(InspectionRepository inspections, SchemaRepository schemas, Clock clock,
                           EventPublisher events) {
        this.inspections = inspections;
        this.schemas = schemas;
        this.clock = clock;
        this.events = events;
    }

    public void execute(InspectionId inspectionId) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        InspectionSchema schema = schemas.findById(inspection.getSchemaId())
                .orElseThrow(() -> new NotFoundException("el esquema", inspection.getSchemaId().value()));
        SchemaVersion current = schema.latestVersion()
                .orElseThrow(() -> new DomainException("El esquema no tiene versiones publicadas"));
        inspection.start(current, clock.now());
        inspections.save(inspection);
        inspection.pullEvents().forEach(events::publish);
    }
}
