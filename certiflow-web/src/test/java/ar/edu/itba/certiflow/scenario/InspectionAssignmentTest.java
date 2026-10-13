package ar.edu.itba.certiflow.scenario;

import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetResponsibleCannotAssignInspectionException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetResponsibleCannotInspectOwnAssetException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssignerCannotInspectException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import org.junit.jupiter.api.Test;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;

class InspectionAssignmentTest extends ScenarioTest {

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
        fixture.assignInspection.execute(new AssignInspectionCommand(fixture.extinguisher.code(), (new InspectionAssignment(inspector, CertiflowFixture.TODAY.plusDays(3), new FullScope())).inspector(), (new InspectionAssignment(inspector, CertiflowFixture.TODAY.plusDays(3), new FullScope())).plannedDate(), (new InspectionAssignment(inspector, CertiflowFixture.TODAY.plusDays(3), new FullScope())).scope(), assignedBy));
    }
}
