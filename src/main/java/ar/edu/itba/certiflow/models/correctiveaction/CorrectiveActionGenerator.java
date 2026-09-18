package ar.edu.itba.certiflow.models.correctiveaction;

import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionPolicy;
import java.util.Collection;
import java.util.List;

/** Derives corrective actions from findings using the policy of their source criterion. */
public class CorrectiveActionGenerator {
    public List<CorrectiveAction> generateFrom(Collection<Finding> findings) {
        return findings.stream()
                .flatMap(finding -> finding.evaluation().criterion().correctiveActionPolicy()
                        .create(finding)
                        .stream())
                .toList();
    }
}
