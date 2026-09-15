package ar.edu.itba.certiflow.domain.model.asset;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.EventSource;
import ar.edu.itba.certiflow.domain.model.shared.PendingEvents;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Asset implements EventSource {

    private final AssetId id;
    private final AssetType type;
    private Location location;
    private PersonId responsible;
    private final Characteristics characteristics;
    @Getter(AccessLevel.NONE)
    private final PendingEvents events = new PendingEvents();

    public void relocate(Location newLocation, LocalDateTime now) {
        events.add(new AssetRelocated(id, location, newLocation, now));
        this.location = newLocation;
    }

    public void reassignResponsible(PersonId newResponsible, LocalDateTime now) {
        events.add(new AssetResponsibleReassigned(id, responsible, newResponsible, now));
        this.responsible = newResponsible;
    }

    @Override
    public List<DomainEvent> pullEvents() {
        return events.pull();
    }
}
