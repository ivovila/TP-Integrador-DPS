package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.Verification;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
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
    public Verification execute(VerifyCorrectiveActionCommand command) {
        Finding finding = findingRepository.findById(command.findingId())
                .orElseThrow(() -> new FindingNotFoundException(command.findingId()));
        Verification verification = findingService.verifyAction(finding, command.actionId(), command.result(), command.notes(),
                new Stamp(command.verifier(), clock.instant()));
        findingRepository.save(finding);
        return verification;
    }
}
