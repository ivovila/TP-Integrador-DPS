package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import java.util.List;

public record FindingsSummary(AssetCode assetCode, List<FindingLine> findings) {

    public FindingsSummary {
        findings = List.copyOf(findings);
    }

    public long openFindings() {
        return findings.stream().filter(FindingLine::open).count();
    }
}
