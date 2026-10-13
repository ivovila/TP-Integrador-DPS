package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.usecase.FindingsSummary;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class FindingsSummaryResource extends RepresentationModel<FindingsSummaryResource> {

    public record Line(String criterionCode, String description, String severity, PersonPayload responsible, boolean open,
                       boolean blocksCertification, long overdueActions) {
    }

    public final String assetCode;
    public final List<Line> findings;
    public final long openFindings;
    public final long blockingFindings;

    FindingsSummaryResource(FindingsSummary summary) {
        this.assetCode = summary.assetCode().value();
        this.findings = summary.findings().stream()
                .map(line -> new Line(line.criterionCode(), line.description(), line.severity(), PersonPayload.of(line.responsible()),
                        line.open(), line.blocksCertification(), line.overdueActions()))
                .toList();
        this.openFindings = summary.openFindings();
        this.blockingFindings = summary.blockingFindings();
    }
}
