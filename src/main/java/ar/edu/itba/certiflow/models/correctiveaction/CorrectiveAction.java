package ar.edu.itba.certiflow.models.correctiveaction;

import ar.edu.itba.certiflow.models.finding.Finding;
import java.util.UUID;

/** A corrective action required to resolve a finding. */
public record CorrectiveAction(UUID id, Finding finding) {
}
