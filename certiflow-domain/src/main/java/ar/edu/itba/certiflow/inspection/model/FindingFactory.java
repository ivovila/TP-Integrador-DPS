package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.Optional;

public final class FindingFactory {

    public Finding create(FindingId id, InspectionId inspectionId, AssetCode assetCode, Person responsible,
                          CriterionResponse<?> nonConformity, Person raisedBy, Instant raisedAt, Optional<SubsystemCode> subsystemCode) {
        return new Finding(id, inspectionId, assetCode, responsible, nonConformity, raisedBy, raisedAt, subsystemCode);
    }

    public Finding restore(FindingSnapshot snapshot) {
        return new Finding(snapshot);
    }
}
