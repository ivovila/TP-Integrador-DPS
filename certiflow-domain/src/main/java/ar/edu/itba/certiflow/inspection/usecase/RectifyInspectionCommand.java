package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record RectifyInspectionCommand(InspectionId inspectionId, String criterionCode, RawAnswer answer, String reason, Person rectifiedBy) implements LocksAsset {
    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.inspection(inspectionId);
    }
}
