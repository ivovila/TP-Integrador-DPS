package ar.edu.itba.certiflow.domain.model.finding;

import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record FindingDetails(CriterionId criterion, Severity severity, String description, PersonId responsible) {
}
