package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.shared.model.Person;

public record FindingLine(String criterionCode, String description, String severity, Person responsible,
                          boolean open, boolean blocksCertification, long overdueActions) {
}
