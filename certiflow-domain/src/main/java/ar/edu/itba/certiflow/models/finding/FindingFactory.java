package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class FindingFactory {

    public Finding create(FindingId id, InspectionId inspectionId, AssetCode assetCode, Person responsible,
                          CriterionResponse<?> nonConformity, Person raisedBy, Instant raisedAt) {
        return new Finding(id, inspectionId, assetCode, responsible, nonConformity, raisedBy, raisedAt);
    }
}
