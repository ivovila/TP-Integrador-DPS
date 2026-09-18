package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.domain.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.finding.Verification;
import ar.edu.itba.certiflow.domain.finding.VerificationResult;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;

public final class VerifyCorrectiveAction {

    private final FindingRepository findings;
    private final Clock clock;

    public VerifyCorrectiveAction(FindingRepository findings, Clock clock) {
        this.findings = findings;
        this.clock = clock;
    }

    public Verification execute(Finding finding, CorrectiveAction action, Person verifier, VerificationResult result,
                                String notes) {
        Verification verification = finding.verifyAction(action, verifier, result, notes, clock.instant());
        findings.save(finding);
        return verification;
    }
}
