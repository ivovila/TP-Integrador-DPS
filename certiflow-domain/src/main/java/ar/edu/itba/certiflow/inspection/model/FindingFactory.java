package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public final class FindingFactory {

    public Finding create(FindingId id, InspectionId inspectionId, AssetCode assetCode, Person responsible,
                          CriterionResponse<?> nonConformity, Person raisedBy, Instant raisedAt) {
        return new Finding(id, inspectionId, assetCode, responsible, nonConformity, raisedBy, raisedAt);
    }

    public Finding restore(FindingSnapshot snapshot) {
        return new Finding(snapshot);
    }
}
