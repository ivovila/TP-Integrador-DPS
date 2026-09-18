package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.shared.Person;

public record FindingLine(String criterionCode, String description, String severity, Person responsible,
                          boolean open, boolean blocksCertification, long overdueActions) {
}
