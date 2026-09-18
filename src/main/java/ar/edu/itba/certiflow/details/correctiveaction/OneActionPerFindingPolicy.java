package ar.edu.itba.certiflow.details.correctiveaction;

import ar.edu.itba.certiflow.models.correctiveaction.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionPolicy;
import ar.edu.itba.certiflow.models.finding.Finding;
import java.util.List;
import java.util.UUID;

/** Requires one corrective action for every finding. */
public class OneActionPerFindingPolicy implements CorrectiveActionPolicy {
    @Override
    public List<CorrectiveAction> create(Finding finding) {
        return List.of(new CorrectiveAction(UUID.randomUUID(), finding));
    }
}
