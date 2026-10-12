package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.Verification;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionUseCase;
import java.time.Clock;

public final class VerifyCorrectiveActionUseCaseImpl implements VerifyCorrectiveActionUseCase {

    private final FindingRepository findingRepository;
    private final FindingService findingService;
    private final Clock clock;

    public VerifyCorrectiveActionUseCaseImpl(FindingRepository findingRepository, FindingService findingService, Clock clock) {
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
