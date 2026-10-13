package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Verification;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class VerificationEmbeddable {

    @Column(name = "verifier_id")
    private String verifierId;
    @Column(name = "verifier_name")
    private String verifierName;
    private String result;
    private String notes;
    @Column(name = "verified_at")
    private Instant verifiedAt;

    protected VerificationEmbeddable() {
    }

    VerificationEmbeddable(Verification verification) {
        PersonColumns verifier = new PersonColumns(verification.verifier());
        this.verifierId = verifier.id();
        this.verifierName = verifier.name();
        this.result = verification.result().name();
        this.notes = verification.notes();
        this.verifiedAt = verification.verifiedAt();
    }

    Verification toDomain() {
        return new Verification(VerificationResult.valueOf(result), notes,
                new Stamp(new PersonColumns(verifierId, verifierName).toDomain(), verifiedAt));
    }
}
