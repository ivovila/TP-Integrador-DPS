package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;

public interface PlanCorrectiveActionUseCase {

    CorrectiveActionId execute(PlanCorrectiveActionCommand command);
}
