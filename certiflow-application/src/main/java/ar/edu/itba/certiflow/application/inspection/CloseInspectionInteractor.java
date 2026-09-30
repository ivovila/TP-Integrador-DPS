package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingFactory;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.inspection.CloseInspection;
import java.time.Clock;
import java.util.List;

public final class CloseInspectionInteractor implements CloseInspection {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final FindingFactory findingFactory;
    private final AuditLog<Inspection> inspectionLog;
    private final AuditLog<Finding> findingLog;
    private final Clock clock;

    public CloseInspectionInteractor(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                     FindingFactory findingFactory, AuditLog<Inspection> inspectionLog, AuditLog<Finding> findingLog, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.findingFactory = findingFactory;
        this.inspectionLog = inspectionLog;
        this.findingLog = findingLog;
        this.clock = clock;
    }

    @Override
    public List<FindingId> execute(InspectionId inspectionId, Person closedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        List<Finding> raisedFindings = findingFactory.create(inspection.close(closedBy, clock.instant()), findingRepository::nextId);
        inspectionRepository.save(inspection);
        inspectionLog.recordAll(inspection, inspection.pullAuditEntries());
        findingRepository.saveAll(raisedFindings);
        raisedFindings.forEach(finding -> findingLog.recordAll(finding, finding.pullAuditEntries()));
        return raisedFindings.stream().map(Finding::id).toList();
    }
}
