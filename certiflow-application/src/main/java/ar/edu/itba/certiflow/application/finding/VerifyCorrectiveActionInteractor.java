package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.Verification;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.usecases.finding.VerifyCorrectiveAction;
import java.time.Clock;

public final class VerifyCorrectiveActionInteractor implements VerifyCorrectiveAction {

    private final FindingRepository findingRepository;
    private final AuditLog<Finding> findingLog;
    private final Clock clock;

    public VerifyCorrectiveActionInteractor(FindingRepository findingRepository, AuditLog<Finding> findingLog, Clock clock) {
        this.findingRepository = findingRepository;
        this.findingLog = findingLog;
        this.clock = clock;
    }

    @Override
    public Verification execute(FindingId findingId, CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult,
                                String verificationNotes) {
        Finding finding = findingRepository.findById(findingId).orElseThrow(() -> new FindingNotFoundException(findingId));
        Verification verification = finding.verifyAction(actionId, verifier, verificationResult, verificationNotes, clock.instant());
        findingRepository.save(finding);
        findingLog.recordAll(finding, finding.pullAuditEntries());
        return verification;
    }
}
