package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record CloseInspectionCommand(InspectionId inspectionId, Person closedBy) implements LocksAsset {
    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.inspection(inspectionId);
    }
}
