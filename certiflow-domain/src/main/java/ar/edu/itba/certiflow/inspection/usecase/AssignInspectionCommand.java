package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

public record AssignInspectionCommand(AssetCode assetCode, Person inspector, LocalDate plannedDate, InspectionScope scope, Person assignedBy) {

    public InspectionAssignment assignment() {
        return new InspectionAssignment(inspector, plannedDate, scope);
    }
}
