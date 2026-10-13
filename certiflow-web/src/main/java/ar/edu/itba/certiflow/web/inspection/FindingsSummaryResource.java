package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.usecase.FindingsSummary;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class FindingsSummaryResource extends RepresentationModel<FindingsSummaryResource> {

    public record Line(String findingId, String criterionCode, String description, String severity, PersonPayload responsible,
                       boolean open, long overdueActions, String subsystemCode) {
    }

    public final String assetCode;
    public final List<Line> findings;
    public final long openFindings;

    FindingsSummaryResource(FindingsSummary summary) {
        this.assetCode = summary.assetCode().value();
        this.findings = summary.findings().stream()
                .map(line -> new Line(line.findingId().value(), line.criterionCode(), line.description(), line.severity(),
                        PersonPayload.of(line.responsible()), line.open(), line.overdueActions(),
                        line.subsystemCode().map(subsystemCode -> subsystemCode.value()).orElse(null)))
                .toList();
        this.openFindings = summary.openFindings();
    }
}
