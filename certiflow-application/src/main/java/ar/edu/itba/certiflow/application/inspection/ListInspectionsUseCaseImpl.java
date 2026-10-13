package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.ListInspectionsUseCase;
import java.util.List;
import java.util.Optional;

public final class ListInspectionsUseCaseImpl implements ListInspectionsUseCase {

    private final InspectionRepository inspectionRepository;

    public ListInspectionsUseCaseImpl(InspectionRepository inspectionRepository) {
        this.inspectionRepository = inspectionRepository;
    }

    @Override
    public List<InspectionView> execute(Optional<AssetCode> asset) {
        return List.copyOf(asset.map(inspectionRepository::findByAsset).orElseGet(inspectionRepository::findAll));
    }
}
