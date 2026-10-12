package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.Verification;

public interface VerifyCorrectiveActionUseCase {

    Verification execute(VerifyCorrectiveActionCommand command);
}
