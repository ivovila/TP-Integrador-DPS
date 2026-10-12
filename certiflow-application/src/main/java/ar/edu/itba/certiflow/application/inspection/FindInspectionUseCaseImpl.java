package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;

public final class FindInspectionUseCaseImpl implements FindInspectionUseCase {

    private final InspectionRepository inspectionRepository;

    public FindInspectionUseCaseImpl(InspectionRepository inspectionRepository) {
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    public InspectionView execute(InspectionId id) {
        return inspectionRepository.findById(id).orElseThrow(() -> new InspectionNotFoundException(id));
    }
}
