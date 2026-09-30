package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.evaluation.Severity;
import ar.edu.itba.certiflow.models.inspection.AttachedEvidence;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface Finding {

    CorrectiveAction planAction(String description, Person responsible, LocalDate dueDate, Person plannedBy, Instant plannedAt,
                                LocalDate plannedOn);

    Verification verifyAction(CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult, String verificationNotes,
                              Instant verifiedAt);

    boolean isOpen();

    boolean blocksCertification();

    boolean concerns(Asset asset);

    boolean raisedFrom(InspectionId sourceInspection, CriterionResponse<?> nonConformity);

    FindingId id();

    InspectionId inspectionId();

    Criterion<?> criterion();

    Severity severity();

    List<AttachedEvidence> evidence();

    Person responsible();

    List<CorrectiveAction> actions();
}
