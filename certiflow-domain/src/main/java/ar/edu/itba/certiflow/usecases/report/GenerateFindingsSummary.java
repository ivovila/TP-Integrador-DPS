package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.usecases.report.FindingsSummary;
import ar.edu.itba.certiflow.models.asset.AssetCode;

public interface GenerateFindingsSummary {

    FindingsSummary execute(AssetCode assetCode);
}
