package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.FindFindingUseCase;

public final class FindFindingUseCaseImpl implements FindFindingUseCase {

    private final FindingRepository findingRepository;

    public FindFindingUseCaseImpl(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    @Override
    public Finding execute(FindingId id) {
        return findingRepository.findById(id).orElseThrow(() -> new FindingNotFoundException(id));
    }
}
