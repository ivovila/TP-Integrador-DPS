package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.FindingLine;
import ar.edu.itba.certiflow.inspection.usecase.FindingsSummary;
import ar.edu.itba.certiflow.inspection.usecase.GenerateFindingsSummaryUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GenerateFindingsSummaryUseCaseImpl implements GenerateFindingsSummaryUseCase {

    private final AssetRepository assetRepository;
    private final FindingRepository findingRepository;
    private final Clock clock;

    public GenerateFindingsSummaryUseCaseImpl(AssetRepository assetRepository, FindingRepository findingRepository, Clock clock) {
        this.assetRepository = assetRepository;
        this.findingRepository = findingRepository;
        this.clock = clock;
    }

    @Override
    public FindingsSummary execute(AssetCode assetCode) {
        Asset asset = assetRepository.findByCode(assetCode).orElseThrow(() -> new AssetNotFoundException(assetCode));
        LocalDate today = LocalDate.now(clock);
        List<Finding> findingsOfAsset = findingRepository.findByAsset(assetCode);
        List<FindingLine> findingLines = findingsOfAsset.stream().map(finding -> lineOf(finding, today)).toList();
        return new FindingsSummary(asset.code(), findingLines,
                findingsOfAsset.stream().filter(Finding::isOpen).count());
    }

    private FindingLine lineOf(Finding finding, LocalDate today) {
        long overdueActions = finding.actions().stream().filter(correctiveAction -> correctiveAction.isOverdueOn(today)).count();
        return new FindingLine(finding.criterion().code(), finding.criterion().text(), finding.severity().label(),
                finding.responsible(), finding.isOpen(), overdueActions);
    }
}
