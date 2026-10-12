package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.Finding;
import java.util.List;
import java.util.Optional;

public interface ListFindingsUseCase {

    List<Finding> execute(Optional<AssetCode> asset);
}
