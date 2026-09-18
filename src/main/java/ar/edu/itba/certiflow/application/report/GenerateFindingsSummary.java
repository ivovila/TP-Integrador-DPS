package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.application.finding.FindingRepository;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.finding.Finding;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class GenerateFindingsSummary {

    private final FindingRepository findings;
    private final Clock clock;

    public GenerateFindingsSummary(FindingRepository findings, Clock clock) {
        this.findings = findings;
        this.clock = clock;
    }

    public FindingsSummary execute(Asset asset) {
        LocalDate today = LocalDate.now(clock);
        List<Finding> ofAsset = findings.findByAsset(asset);
        List<FindingLine> lines = ofAsset.stream().map(finding -> lineOf(finding, today)).toList();
        return new FindingsSummary(asset.code(), lines,
                ofAsset.stream().filter(Finding::isOpen).count(),
                ofAsset.stream().filter(Finding::blocksCertification).count());
    }

    private FindingLine lineOf(Finding finding, LocalDate today) {
        long overdue = finding.actions().stream().filter(action -> action.isOverdueOn(today)).count();
        return new FindingLine(finding.criterion().code(), finding.criterion().text(), finding.severity().label(),
                finding.responsible(), finding.isOpen(), finding.blocksCertification(), overdue);
    }
}
