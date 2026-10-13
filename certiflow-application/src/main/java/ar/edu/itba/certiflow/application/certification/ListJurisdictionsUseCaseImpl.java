package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.JurisdictionPolicy;
import ar.edu.itba.certiflow.certification.model.RegisteredJurisdictions;
import ar.edu.itba.certiflow.certification.usecase.ListJurisdictionsUseCase;
import java.util.List;

public final class ListJurisdictionsUseCaseImpl implements ListJurisdictionsUseCase {

    private final RegisteredJurisdictions registeredJurisdictions;

    public ListJurisdictionsUseCaseImpl(RegisteredJurisdictions registeredJurisdictions) {
        this.registeredJurisdictions = registeredJurisdictions;
    }

    @Override
    public List<JurisdictionPolicy> execute() {
        return registeredJurisdictions.all();
    }
}
