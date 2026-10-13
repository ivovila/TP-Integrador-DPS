package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.Person;

public record FindingLine(FindingId findingId, String criterionCode, String description, String severity, Person responsible,
                          boolean open, long overdueActions, SubsystemAssociation subsystem) {

}
