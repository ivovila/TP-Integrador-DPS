package ar.edu.itba.certiflow.domain.model.asset;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record AssetResponsibleReassigned(AssetId assetId, PersonId previous, PersonId current,
                                         LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public AssetId aggregateId() {
        return assetId;
    }
}
