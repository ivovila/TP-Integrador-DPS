package ar.edu.itba.certiflow.harness;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewFactory;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.harness.golden.CaseInspections;
import ar.edu.itba.certiflow.harness.golden.GoldenCase;
import ar.edu.itba.certiflow.harness.golden.GoldenCase.ExpectedFinding;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Corre cada caso varias veces contra el asistente y lo mide. Cada respuesta pasa por {@link AssistantReviewFactory},
 * la misma fábrica que usa la aplicación, así las métricas son sobre lo que el inspector vería, después de los
 * guardrails, y no sobre la respuesta cruda del modelo.
 */
public final class HarnessRun {

    private static final Person HARNESS = new Person(new PersonId("HARNESS"), "Evaluation harness");

    private final InspectionAssistant assistant;
    private final CaseInspections caseInspections;
    private final AssistantReviewFactory reviewFactory = new AssistantReviewFactory();
    private final Scoring scoring = new Scoring();
    private final Clock clock;
    private final Consumer<String> progress;

    public HarnessRun(InspectionAssistant assistant, CaseInspections caseInspections, Clock clock, Consumer<String> progress) {
        this.assistant = assistant;
        this.caseInspections = caseInspections;
        this.clock = clock;
        this.progress = progress;
    }

    public HarnessReport run(List<GoldenCase> cases, int runsPerCase) {
        List<AssistantReview> reviews = new ArrayList<>();
        List<CaseResult> results = cases.stream().map(goldenCase -> measure(goldenCase, runsPerCase, reviews)).toList();
        AssistantReview labels = reviews.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("The assistant did not answer a single run"));
        return new HarnessReport(labels.promptVersion().value(), labels.modelId().value(), runsPerCase, clock.instant(),
                results);
    }

    private CaseResult measure(GoldenCase goldenCase, int runsPerCase, List<AssistantReview> reviews) {
        InspectionUnderReview inspection = caseInspections.underReview(goldenCase);
        List<RunResult> runs = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        IntStream.rangeClosed(1, runsPerCase).forEach(run -> review(goldenCase, inspection, run, failures)
                .ifPresent(review -> {
                    reviews.add(review);
                    runs.add(scoring.score(run, goldenCase, review));
                }));
        progress.accept(goldenCase.id() + ": " + runs.size() + " of " + runsPerCase + " runs measured");
        return new CaseResult(goldenCase.id(), goldenCase.kind(), goldenCase.description(),
                goldenCase.expected().stream().map(this::describe).toList(),
                runs, failures, new EvaluationFingerprint().of(inspection, goldenCase));
    }

    /** Un proveedor que falla no corta la corrida: esa vuelta queda como falla del caso y sigue la siguiente. */
    private Optional<AssistantReview> review(GoldenCase goldenCase, InspectionUnderReview inspection, int run,
                                             List<String> failures) {
        try {
            return Optional.of(reviewFactory.create(new ReviewId(goldenCase.id() + "-run-" + run), inspection,
                    assistant.review(inspection), new Stamp(HARNESS, clock.instant())));
        } catch (AssistantUnavailableException failure) {
            failures.add("run " + run + ": " + Optional.ofNullable(failure.getCause()).map(Throwable::getMessage)
                    .orElse(failure.getMessage()));
            return Optional.empty();
        }
    }

    private String describe(ExpectedFinding expected) {
        return expected.criterion() + " " + expected.severity();
    }
}
