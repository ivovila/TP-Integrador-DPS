package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;

public interface GenerateFindingsSummaryUseCase {

    FindingsSummary execute(AssetCode assetCode);
}
