package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import java.util.List;

public record FindingsSummary(AssetCode assetCode, List<FindingLine> findings, long openFindings,
                              long blockingFindings) {

    public FindingsSummary {
        findings = List.copyOf(findings);
    }
}
