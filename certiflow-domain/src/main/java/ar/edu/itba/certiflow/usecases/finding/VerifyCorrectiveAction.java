package ar.edu.itba.certiflow.usecases.finding;

import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.Verification;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.shared.Person;

public interface VerifyCorrectiveAction {

    Verification execute(FindingId findingId, CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult,
                         String verificationNotes);
}
