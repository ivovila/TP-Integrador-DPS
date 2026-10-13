package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record VerifyCorrectiveActionCommand(FindingId findingId, CorrectiveActionId actionId, Person verifier, VerificationResult result, String notes) implements LocksAsset {
    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.finding(findingId);
    }
}
