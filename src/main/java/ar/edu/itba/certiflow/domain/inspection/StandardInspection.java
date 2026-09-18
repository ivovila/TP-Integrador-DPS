package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StandardInspection implements Inspection {

    private static final String CLOSURE_REASON = "Inspection closed";

    private final Asset asset;
    private final InspectionAssignment assignment;
    private final SchemaVersion schemaVersion;
    private final Map<Criterion<?>, CriterionResponse<?>> responses = new LinkedHashMap<>();
    private final List<Revision> revisions = new ArrayList<>();

    StandardInspection(Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion) {
        this.asset = asset;
        this.assignment = assignment;
        this.schemaVersion = schemaVersion;
        schemaVersion.criteria().forEach(criterion -> responses.put(criterion, new CriterionResponse<>(criterion)));
    }

    @Override
    public <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person by, Instant at) {
        ensureOpen();
        replaceAnswer(criterion, answer, by, at);
    }

    @Override
    public void attachEvidence(Criterion<?> criterion, Evidence evidence, Person by, Instant at) {
        ensureOpen();
        schemaVersion.ensureContains(criterion);
        responses.put(criterion, responses.get(criterion).withEvidence(evidence, by, at));
    }

    @Override
    public void addObservation(Criterion<?> criterion, String text, Person by, Instant at) {
        ensureOpen();
        schemaVersion.ensureContains(criterion);
        responses.put(criterion, responses.get(criterion).withObservation(new Observation(text, by, at)));
    }

    @Override
    public void close(Person by, Instant at) {
        ensureOpen();
        Evaluation evaluation = evaluate();
        evaluation.ensureComplete();
        revisions.add(new Revision(1, CLOSURE_REASON, evaluation, by, at));
    }

    @Override
    public <A extends Answer> void rectify(Criterion<A> criterion, A answer, String reason, Person by, Instant at) {
        ensureClosed();
        if (reason.isBlank()) {
            throw new RectificationReasonRequiredException();
        }
        replaceAnswer(criterion, answer, by, at);
        revisions.add(new Revision(revisions.size() + 1, reason, evaluate(), by, at));
    }

    @Override
    public Evaluation evaluate() {
        return new Evaluation(List.copyOf(responses.values()));
    }

    @Override
    public boolean isClosed() {
        return !revisions.isEmpty();
    }

    @Override
    public Revision originalRevision() {
        ensureClosed();
        return revisions.getFirst();
    }

    @Override
    public List<Revision> revisions() {
        return List.copyOf(revisions);
    }

    @Override
    public Asset asset() {
        return asset;
    }

    @Override
    public InspectionAssignment assignment() {
        return assignment;
    }

    @Override
    public SchemaVersion schemaVersion() {
        return schemaVersion;
    }

    private <A extends Answer> void replaceAnswer(Criterion<A> criterion, A answer, Person by, Instant at) {
        schemaVersion.ensureContains(criterion);
        responses.put(criterion, responses.get(criterion).answeredWith(criterion, answer, by, at));
    }

    private void ensureOpen() {
        if (isClosed()) {
            throw new InspectionAlreadyClosedException();
        }
    }

    private void ensureClosed() {
        if (!isClosed()) {
            throw new InspectionNotClosedException();
        }
    }
}
