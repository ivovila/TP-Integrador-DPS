package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.FindingService;
import ar.edu.itba.certiflow.models.finding.Verification;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.usecases.finding.VerifyCorrectiveAction;
import java.time.Clock;

public final class VerifyCorrectiveActionInteractor implements VerifyCorrectiveAction {

    private final FindingRepository findingRepository;
    private final FindingService findingService;
    private final Clock clock;

    public VerifyCorrectiveActionInteractor(FindingRepository findingRepository, FindingService findingService, Clock clock) {
        this.findingRepository = findingRepository;
        this.findingService = findingService;
        this.clock = clock;
    }

    @Override
    public Verification execute(FindingId findingId, CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult,
                                String verificationNotes) {
        Finding finding = findingRepository.findById(findingId).orElseThrow(() -> new FindingNotFoundException(findingId));
        Verification verification = findingService.verifyAction(finding, actionId, verifier, verificationResult,
                verificationNotes, clock.instant());
        findingRepository.save(finding);
        return verification;
    }
}
