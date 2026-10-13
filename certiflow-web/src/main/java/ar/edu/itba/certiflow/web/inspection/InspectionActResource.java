package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.inspection.usecase.InspectionAct;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class InspectionActResource extends RepresentationModel<InspectionActResource> {

    public record Line(String criterionCode, String criterionText, AnswerPayload answer, PersonPayload answeredBy, Instant answeredAt,
                       int evidenceCount, List<String> observations, String outcome) {
    }

    public final String assetCode;
    public final String assetName;
    public final PersonPayload inspector;
    public final LocalDate plannedDate;
    public final int schemaVersion;
    public final List<Line> lines;
    public final List<Integer> revisions;
    public final List<AuditEntryResponse> history;

    InspectionActResource(InspectionAct act) {
        this.assetCode = act.assetCode().value();
        this.assetName = act.assetName();
        this.inspector = PersonPayload.of(act.assignment().inspector());
        this.plannedDate = act.assignment().plannedDate();
        this.schemaVersion = act.schemaVersion();
        this.lines = act.lines().stream()
                .map(line -> new Line(line.criterionCode(), line.criterionText(), line.answer().describedBy(new AnswerPayloadDescriber()),
                        PersonPayload.of(line.answeredBy()), line.answeredAt(), line.evidenceCount(), line.observations(),
                        line.outcome().name()))
                .toList();
        this.revisions = act.revisions().asList().stream().map(Revision::number).toList();
        this.history = act.history().stream().map(AuditEntryResponse::of).toList();
    }
}
