package ar.edu.itba.certiflow.domain.asset;

import ar.edu.itba.certiflow.domain.shared.PersonId;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Asset {

    private final AssetId id;
    private final AssetType type;
    private Location location;
    private PersonId responsible;
    private final Characteristics characteristics;

    public void relocate(Location newLocation) {
        this.location = newLocation;
    }

    public void reassignResponsible(PersonId newResponsible) {
        this.responsible = newResponsible;
    }
}
