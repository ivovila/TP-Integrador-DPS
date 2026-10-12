package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.ListFindingsUseCase;
import java.util.List;
import java.util.Optional;

public final class ListFindingsUseCaseImpl implements ListFindingsUseCase {

    private final FindingRepository findingRepository;

    public ListFindingsUseCaseImpl(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    @Override
    public List<Finding> execute(Optional<AssetCode> asset) {
        return asset.map(findingRepository::findByAsset).orElseGet(findingRepository::findAll);
    }
}
