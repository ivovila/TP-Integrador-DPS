package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.application.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.application.report.FindingLine;
import ar.edu.itba.certiflow.application.report.FindingsSummary;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.ports.AssetRepository;
import ar.edu.itba.certiflow.ports.FindingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GenerateFindingsSummary {

    private final AssetRepository assetRepository;
    private final FindingRepository findingRepository;
    private final Clock clock;

    public GenerateFindingsSummary(AssetRepository assetRepository, FindingRepository findingRepository, Clock clock) {
        this.assetRepository = assetRepository;
        this.findingRepository = findingRepository;
        this.clock = clock;
    }

    public FindingsSummary execute(AssetCode assetCode) {
        Asset asset = assetRepository.findByCode(assetCode).orElseThrow(() -> new AssetNotFoundException(assetCode));
        LocalDate today = LocalDate.now(clock);
        List<Finding> findingsOfAsset = findingRepository.findByAsset(asset);
        List<FindingLine> findingLines = findingsOfAsset.stream().map(finding -> lineOf(finding, today)).toList();
        return new FindingsSummary(asset.code(), findingLines,
                findingsOfAsset.stream().filter(Finding::isOpen).count(),
                findingsOfAsset.stream().filter(Finding::blocksCertification).count());
    }

    private FindingLine lineOf(Finding finding, LocalDate today) {
        long overdueActions = finding.actions().stream().filter(correctiveAction -> correctiveAction.isOverdueOn(today)).count();
        return new FindingLine(finding.criterion().code(), finding.criterion().text(), finding.severity().label(),
                finding.responsible(), finding.isOpen(), finding.blocksCertification(), overdueActions);
    }
}
