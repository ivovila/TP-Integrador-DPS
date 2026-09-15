package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class RaiseFinding {

    private final InspectionRepository inspections;
    private final FindingRepository findings;
    private final Clock clock;
    private final EventPublisher events;

    public RaiseFinding(InspectionRepository inspections, FindingRepository findings, Clock clock,
                        EventPublisher events) {
        this.inspections = inspections;
        this.findings = findings;
        this.clock = clock;
        this.events = events;
    }

    public Finding execute(InspectionId inspectionId, CriterionId criterionId, Severity severity,
                           String description, PersonId responsible) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        boolean alreadyRaised = findings.findByInspection(inspectionId).stream()
                .anyMatch(finding -> finding.getCriterion().equals(criterionId));
        if (alreadyRaised) {
            throw new DomainException("Ya existe un hallazgo para ese criterio en la inspeccion");
        }
        Finding finding = Finding.raise(FindingId.generate(), inspection, criterionId, severity, description,
                responsible, clock.now());
        findings.save(finding);
        finding.pullEvents().forEach(events::publish);
        return finding;
    }
}
