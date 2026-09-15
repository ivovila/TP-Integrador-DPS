package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.report.FindingsSummary;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;

public class GenerateFindingsSummary {

    private final FindingRepository findings;
    private final Clock clock;

    public GenerateFindingsSummary(FindingRepository findings, Clock clock) {
        this.findings = findings;
        this.clock = clock;
    }

    public FindingsSummary execute(InspectionId inspectionId) {
        return FindingsSummary.of(inspectionId, findings.findByInspection(inspectionId), clock.today());
    }
}
