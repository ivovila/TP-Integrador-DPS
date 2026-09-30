package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.models.inspection.FullScope;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.exceptions.AssetResponsibleCannotAssignInspectionException;
import ar.edu.itba.certiflow.models.inspection.exceptions.AssetResponsibleCannotInspectOwnAssetException;
import ar.edu.itba.certiflow.models.inspection.exceptions.AssignerCannotInspectException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import org.junit.jupiter.api.Test;

class InspectionAssignmentTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void whoeverAssignsTheInspectionCannotAlsoPerformIt() {
        assertThrows(AssignerCannotInspectException.class, () -> assign(fixture.planner, fixture.planner));
    }

    @Test
    void assetResponsibleCannotAssignTheInspectionOfTheirAsset() {
        assertThrows(AssetResponsibleCannotAssignInspectionException.class, () -> assign(fixture.inspector, fixture.assetResponsible));
    }

    @Test
    void assetResponsibleCannotInspectTheirOwnAsset() {
        assertThrows(AssetResponsibleCannotInspectOwnAssetException.class, () -> assign(fixture.assetResponsible, fixture.planner));
    }

    private void assign(Person inspector, Person assignedBy) {
        fixture.assignInspection.execute(fixture.extinguisher.code(),
                new InspectionAssignment(inspector, CertiflowFixture.TODAY.plusDays(3), new FullScope()), assignedBy);
    }
}
