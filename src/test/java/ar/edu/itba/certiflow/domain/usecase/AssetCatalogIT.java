package ar.edu.itba.certiflow.domain.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetRelocated;
import ar.edu.itba.certiflow.domain.model.asset.AssetResponsibleReassigned;
import ar.edu.itba.certiflow.domain.model.asset.Location;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.support.Fixtures;
import ar.edu.itba.certiflow.support.TestContext;

class AssetCatalogIT {

    private TestContext ctx;
    private Asset asset;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        Asset template = Fixtures.extinguisher();
        asset = ctx.registerAsset.execute(template.getType(), template.getLocation(), template.getResponsible(),
                template.getCharacteristics());
    }

    @Test
    void assetModificationsAreKeptInTheHistory() {
        Location previousLocation = asset.getLocation();
        Location warehouse = new Location("Planta Sur", "Ruta 8 km 50", "Galpon 2");
        PersonId previousResponsible = asset.getResponsible();
        PersonId newResponsible = PersonId.generate();

        ctx.relocateAsset.execute(asset.getId(), warehouse);
        ctx.clock.advanceDays(3);
        ctx.reassignResponsible.execute(asset.getId(), newResponsible);

        Asset stored = ctx.assets.getById(asset.getId());
        assertEquals(warehouse, stored.getLocation());
        assertEquals(newResponsible, stored.getResponsible());

        List<DomainEvent> history = ctx.audit.history(asset.getId());
        AssetRelocated relocated = assertInstanceOf(AssetRelocated.class, history.get(0));
        AssetResponsibleReassigned reassigned = assertInstanceOf(AssetResponsibleReassigned.class, history.get(1));
        assertEquals(previousLocation, relocated.from());
        assertEquals(previousResponsible, reassigned.previous());
        assertEquals(ctx.clock.now(), reassigned.occurredAt());
    }
}
