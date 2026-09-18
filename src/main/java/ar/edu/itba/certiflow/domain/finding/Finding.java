package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.Severity;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
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

    Inspection inspection();

    Criterion<?> criterion();

    Severity severity();

    List<Evidence> evidence();

    Person responsible();

    List<CorrectiveAction> actions();
}
