package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.correctiveaction.CorrectiveAction;
import java.util.List;

@FunctionalInterface
public interface CorrectiveActionPolicy {
    List<CorrectiveAction> create(Finding finding);
}
