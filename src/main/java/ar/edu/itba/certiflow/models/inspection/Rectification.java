package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.people.Inspector;

public record Rectification(String reason, Inspector actor) {
}
