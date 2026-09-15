package ar.edu.itba.certiflow.domain.model.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Response {

    private final List<RecordedValue> values;
    private final List<Evidence> evidences;
    private final List<String> observations;

    private Response(List<RecordedValue> values, List<Evidence> evidences, List<String> observations) {
        this.values = List.copyOf(values);
        this.evidences = List.copyOf(evidences);
        this.observations = List.copyOf(observations);
    }

    public static Response empty() {
        return new Response(List.of(), List.of(), List.of());
    }

    public Response with(RecordedValue value) {
        List<RecordedValue> updated = new ArrayList<>(values);
        updated.removeIf(value::supersedes);
        updated.add(value);
        return new Response(updated, evidences, observations);
    }

    public Response withEvidence(Evidence evidence) {
        List<Evidence> updated = new ArrayList<>(evidences);
        updated.add(evidence);
        return new Response(values, updated, observations);
    }

    public Response withObservation(String observation) {
        List<String> updated = new ArrayList<>(observations);
        updated.add(observation);
        return new Response(values, evidences, updated);
    }

    public <T extends RecordedValue> List<T> all(Class<T> type) {
        return values.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    public List<RecordedValue> values() {
        return values;
    }

    public List<Evidence> evidences() {
        return evidences;
    }

    public List<String> observations() {
        return observations;
    }

    public Set<EvidenceType> evidenceTypes() {
        return evidences.stream().map(Evidence::type).collect(Collectors.toUnmodifiableSet());
    }
}
