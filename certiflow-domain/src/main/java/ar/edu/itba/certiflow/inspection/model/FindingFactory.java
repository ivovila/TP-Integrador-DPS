package ar.edu.itba.certiflow.inspection.model;

public final class FindingFactory {

    public Finding create(FindingId id, RaisesFindings source, CriterionResponse<?> nonConformity) {
        return new Finding(id, source, nonConformity);
    }

    public Finding restore(FindingSnapshot snapshot) {
        return new Finding(snapshot);
    }
}
