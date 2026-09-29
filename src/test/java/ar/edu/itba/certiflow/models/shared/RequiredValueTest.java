package ar.edu.itba.certiflow.models.shared;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.asset.Location;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequiredValueTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void personWithoutNameOrIdIsADomainError() {
        assertThrows(RequiredValueMissingException.class, () -> new Person(new PersonId("EMP-100"), " "));
        assertThrows(RequiredValueMissingException.class, () -> new PersonId(" "));
    }

    @Test
    void assetWithoutNameIsADomainError() {
        assertThrows(RequiredValueMissingException.class, () -> new Asset(new AssetCode("EXT-0043"), " ",
                CertiflowFixture.EXTINGUISHER, new Location("Warehouse"), fixture.assetResponsible, Map.of()));
    }

    @Test
    void correctiveActionWithoutDescriptionIsADomainError() {
        Finding finding = fixture.majorFindingOf(fixture.assignedInspection());

        assertThrows(RequiredValueMissingException.class, () -> fixture.planCorrectiveAction.execute(finding, " ",
                fixture.maintenanceTechnician, CertiflowFixture.TODAY, fixture.assetResponsible));
    }
}
