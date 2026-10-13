package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import java.util.List;
import java.util.Optional;

public interface ListInspectionsUseCase {

    List<InspectionView> execute(Optional<AssetCode> asset);
}
