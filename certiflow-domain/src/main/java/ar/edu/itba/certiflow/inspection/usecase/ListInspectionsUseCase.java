package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import java.util.List;

public interface ListInspectionsUseCase {

    List<InspectionView> execute();

    List<InspectionView> execute(AssetCode asset);
}
