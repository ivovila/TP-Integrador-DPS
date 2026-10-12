package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.Verification;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.shared.model.Person;

public interface VerifyCorrectiveActionUseCase {

    Verification execute(FindingId findingId, CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult,
                         String verificationNotes);
}
