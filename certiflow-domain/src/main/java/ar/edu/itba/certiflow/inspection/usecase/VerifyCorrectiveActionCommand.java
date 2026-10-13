package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.shared.model.Person;

public record VerifyCorrectiveActionCommand(FindingId findingId, CorrectiveActionId actionId, Person verifier,
                                            VerificationResult result, String notes) {
}
