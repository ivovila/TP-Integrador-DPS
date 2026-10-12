package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "correctiveActions", itemRelation = "correctiveAction")
public class CorrectiveActionResource extends RepresentationModel<CorrectiveActionResource> {

    public record VerificationView(PersonPayload verifier, String result, String notes, Instant verifiedAt) {
    }

    public final int id;
    public final String description;
    public final PersonPayload responsible;
    public final LocalDate dueDate;
    public final PersonPayload plannedBy;
    public final Instant plannedAt;
    public final boolean open;
    public final boolean closed;
    public final boolean cancelled;
    public final boolean overdue;
    public final List<VerificationView> verifications;

    CorrectiveActionResource(CorrectiveAction action, LocalDate today) {
        this.id = action.id().value();
        this.description = action.description();
        this.responsible = PersonPayload.of(action.responsible());
        this.dueDate = action.dueDate();
        this.plannedBy = PersonPayload.of(action.plannedBy());
        this.plannedAt = action.plannedAt();
        this.open = action.isOpen();
        this.closed = action.isClosed();
        this.cancelled = action.isCancelled();
        this.overdue = action.isOverdueOn(today);
        this.verifications = action.verifications().stream()
                .map(verification -> new VerificationView(PersonPayload.of(verification.verifier()), verification.result().name(),
                        verification.notes(), verification.verifiedAt()))
                .toList();
    }
}
