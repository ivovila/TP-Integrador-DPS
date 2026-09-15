package ar.edu.itba.certiflow.domain.model.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class Response {

    private final Measurement measurement;
    private final Boolean answer;
    private final String option;
    private final List<Evidence> evidences;
    private final List<String> observations;

    private Response(Measurement measurement, Boolean answer, String option,
                     List<Evidence> evidences, List<String> observations) {
        this.measurement = measurement;
        this.answer = answer;
        this.option = option;
        this.evidences = List.copyOf(evidences);
        this.observations = List.copyOf(observations);
    }

    public static Response empty() {
        return new Response(null, null, null, List.of(), List.of());
    }

    public Response withMeasurement(Measurement newMeasurement) {
        return new Response(newMeasurement, answer, option, evidences, observations);
    }

    public Response withAnswer(boolean newAnswer) {
        return new Response(measurement, newAnswer, option, evidences, observations);
    }

    public Response withOption(String newOption) {
        return new Response(measurement, answer, newOption, evidences, observations);
    }

    public Response withEvidence(Evidence evidence) {
        List<Evidence> updated = new ArrayList<>(evidences);
        updated.add(evidence);
        return new Response(measurement, answer, option, updated, observations);
    }

    public Response withObservation(String observation) {
        List<String> updated = new ArrayList<>(observations);
        updated.add(observation);
        return new Response(measurement, answer, option, evidences, updated);
    }

    public Optional<Measurement> measurement() {
        return Optional.ofNullable(measurement);
    }

    public Optional<Boolean> answer() {
        return Optional.ofNullable(answer);
    }

    public Optional<String> option() {
        return Optional.ofNullable(option);
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
