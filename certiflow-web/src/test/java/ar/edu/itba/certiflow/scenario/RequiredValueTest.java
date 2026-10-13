package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;

class RequiredValueTest extends ScenarioTest {

    @Test
    void personWithoutNameOrIdIsADomainError() {
        assertThrows(RequiredValueMissingException.class, () -> new Person(new PersonId("EMP-100"), " "));
        assertThrows(RequiredValueMissingException.class, () -> new PersonId(" "));
    }

    @Test
    void assetWithoutNameIsADomainError() {
        assertThrows(RequiredValueMissingException.class, () -> new Asset(new AssetCode("EXT-0043"), " ", CertiflowFixture.EXTINGUISHER, new Location("Warehouse"), fixture.assetResponsible, Characteristics.none(), new JurisdictionCode("DEMO-STANDARD"), List.of()));
    }

    @Test
    void correctiveActionWithoutDescriptionIsADomainError() {
        FindingId finding = fixture.majorFindingOf(fixture.assignedInspection());

        assertThrows(RequiredValueMissingException.class, () -> fixture.plan(finding, " ", fixture.maintenanceTechnician, CertiflowFixture.TODAY, fixture.assetResponsible));
    }
}
