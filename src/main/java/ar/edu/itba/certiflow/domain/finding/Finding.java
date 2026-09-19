package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.evaluation.Severity;
import ar.edu.itba.certiflow.domain.inspection.AttachedEvidence;
import ar.edu.itba.certiflow.domain.inspection.InspectionView;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface Finding {

    CorrectiveAction planAction(String description, Person responsible, LocalDate dueDate, Person by, Instant at);

    Verification verifyAction(CorrectiveAction action, Person verifier, VerificationResult result, String notes,
                              Instant at);

    boolean isOpen();

    boolean blocksCertification();

    boolean concerns(Asset asset);

    InspectionView inspection();

    Criterion<?> criterion();

    Severity severity();

    List<AttachedEvidence> evidence();

    Person responsible();

    List<CorrectiveAction> actions();
}
