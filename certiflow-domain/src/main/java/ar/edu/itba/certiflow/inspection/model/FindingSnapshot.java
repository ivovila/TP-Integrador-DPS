package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public record FindingSnapshot(FindingId id, InspectionId inspectionId, AssetCode assetCode, Criterion<?> criterion,
                              List<AttachedEvidence> evidence, Person responsible, List<CorrectiveActionSnapshot> actions) {

    public FindingSnapshot {
        evidence = List.copyOf(evidence);
        actions = List.copyOf(actions);
    }
}
