package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.time.LocalDate;
import java.util.Set;

public record AssignmentRequest(String assetCode, PersonPayload inspector, LocalDate plannedDate, Set<String> sections) {

    public AssignmentRequest {
        RequestFields.require(assetCode, "assetCode");
        RequestFields.require(inspector, "inspector");
        RequestFields.require(plannedDate, "plannedDate");
    }

    AssignInspectionCommand toCommand(Person assignedBy) {
        return new AssignInspectionCommand(new AssetCode(assetCode), inspector.toDomain(), plannedDate,
                sections == null ? new FullScope() : new SectionsScope(sections), assignedBy);
    }
}
