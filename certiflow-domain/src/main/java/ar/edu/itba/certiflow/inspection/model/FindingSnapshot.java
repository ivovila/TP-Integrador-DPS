package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;
import java.util.Optional;

public record FindingSnapshot(FindingId id, InspectionId inspectionId, AssetCode assetCode, Criterion<?> criterion,
                              List<AttachedEvidence> evidence, Person responsible, List<CorrectiveActionSnapshot> actions, SubsystemCode subsystem) {

    /** El subsistema al que pertenece, si tiene uno. */
    public Optional<SubsystemCode> subsystemCode() {
        return Optional.ofNullable(subsystem);
    }


    public FindingSnapshot {
        evidence = List.copyOf(evidence);
        actions = List.copyOf(actions);
    }
}
