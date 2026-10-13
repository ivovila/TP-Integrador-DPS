package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.ListInspectionsUseCase;
import java.util.List;

public final class ListInspectionsUseCaseImpl implements ListInspectionsUseCase {

    private final InspectionRepository inspectionRepository;

    public ListInspectionsUseCaseImpl(InspectionRepository inspectionRepository) {
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    public List<InspectionView> execute() {
        return List.copyOf(inspectionRepository.findAll());
    }

    @Override
    public List<InspectionView> execute(AssetCode asset) {
        return List.copyOf(inspectionRepository.findByAsset(asset));
    }
}
