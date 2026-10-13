package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.Finding;
import java.util.List;

public interface ListFindingsUseCase {

    List<Finding> execute();

    List<Finding> execute(AssetCode asset);
}
