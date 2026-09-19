package ar.edu.itba.certiflow.usecases.finding;

import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.Verification;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import java.time.Clock;

public final class VerifyCorrectiveAction {

    private final FindingRepository findingRepository;
    private final Clock clock;

    public VerifyCorrectiveAction(FindingRepository findingRepository, Clock clock) {
        this.findingRepository = findingRepository;
        this.clock = clock;
    }

    public Verification execute(Finding finding, CorrectiveAction correctiveAction, Person verifier, VerificationResult verificationResult,
                                String verificationNotes) {
        Verification verification = finding.verifyAction(correctiveAction, verifier, verificationResult, verificationNotes, clock.instant());
        findingRepository.save(finding);
        return verification;
    }
}
