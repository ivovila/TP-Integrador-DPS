package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingDetails;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
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

    public Finding execute(InspectionId inspectionId, FindingDetails details) {
        Inspection inspection = inspections.getById(inspectionId);
        Finding finding = Finding.raise(FindingId.generate(), inspection.getEvaluation(), details,
                findings.findByInspection(inspectionId), clock.now());
        findings.save(finding);
        events.publishFrom(finding);
        return finding;
    }
}
