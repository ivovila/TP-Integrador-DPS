package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.application.report.FindingLine;
import ar.edu.itba.certiflow.application.report.FindingsSummary;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.ports.FindingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GenerateFindingsSummary {

    private final FindingRepository findingRepository;
    private final Clock clock;

    public GenerateFindingsSummary(FindingRepository findingRepository, Clock clock) {
        this.findingRepository = findingRepository;
        this.clock = clock;
    }

    public FindingsSummary execute(Asset asset) {
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
