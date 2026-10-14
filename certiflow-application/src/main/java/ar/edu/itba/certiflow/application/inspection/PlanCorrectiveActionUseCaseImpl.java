package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.ActionPlan;
import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public final class PlanCorrectiveActionUseCaseImpl implements PlanCorrectiveActionUseCase {

    private final FindingRepository findingRepository;
    private final FindingService findingService;
    private final AssetLock assetLock;
    private final Clock clock;

    public PlanCorrectiveActionUseCaseImpl(FindingRepository findingRepository, FindingService findingService,
                                           AssetLock assetLock, Clock clock) {
        this.findingRepository = findingRepository;
        this.findingService = findingService;
        this.assetLock = assetLock;
        this.clock = clock;
    }

    @Override
    public CorrectiveActionId execute(PlanCorrectiveActionCommand command) {
        AssetCode assetCode = findingRepository.assetCodeOf(command.findingId())
                .orElseThrow(() -> new FindingNotFoundException(command.findingId()));
        assetLock.lock(assetCode);
        Finding finding = findingRepository.findById(command.findingId())
                .orElseThrow(() -> new FindingNotFoundException(command.findingId()));
        Instant plannedAt = clock.instant();
        CorrectiveAction plannedAction = findingService.planAction(finding,
                new ActionPlan(command.description(), command.responsible(), command.dueDate()),
                new Stamp(command.plannedBy(), plannedAt), LocalDate.ofInstant(plannedAt, clock.getZone()));
        findingRepository.save(finding);
        return plannedAction.id();
    }
}
