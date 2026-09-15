package ar.edu.itba.certiflow.domain.model.asset;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Asset extends AggregateRoot {

    private final AssetId id;
    private final AssetType type;
    private Location location;
    private PersonId responsible;
    private final Characteristics characteristics;

    public void relocate(Location newLocation, LocalDateTime now) {
        recordEvent(new AssetRelocated(id, location, newLocation, now));
        this.location = newLocation;
    }

    public void reassignResponsible(PersonId newResponsible, LocalDateTime now) {
        recordEvent(new AssetResponsibleReassigned(id, responsible, newResponsible, now));
        this.responsible = newResponsible;
    }
}
