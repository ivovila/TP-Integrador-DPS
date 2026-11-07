package ar.edu.itba.certiflow.infrastructure.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * El asistente de CI, de los tests y del desarrollo sin API key: determinista y sin red. Propone un hallazgo en el
 * criterio donde está escrito un texto que nombra una falla, citándolo entero. Es deliberadamente ingenuo: no
 * entiende negaciones ni sabe a qué criterio corresponde lo que lee, así que también sirve de línea de base contra
 * la que el harness compara al modelo real.
 */
public final class StubInspectionAssistant implements InspectionAssistant {

    static final PromptVersion PROMPT_VERSION = new PromptVersion("stub");
    static final ModelId MODEL_ID = new ModelId("keyword-stub");

    private static final List<String> FAILURE_CUES = List.of("grieta", "fisura", "corro", "óxido", "oxid", "fuga",
            "pérdida", "roto", "rota", "dañ", "vencid", "suelt", "tapa", "crack", "leak", "rust", "broken", "damage", "expired",
            "loose");

    @Override
    public AssistantReply review(InspectionUnderReview inspection) {
        return new AssistantReply(PROMPT_VERSION, MODEL_ID,
                inspection.criteria().stream().flatMap(this::proposalsFor).toList());
    }

    private Stream<ProposedFinding> proposalsFor(CriterionUnderReview criterion) {
        return criterion.freeText().stream()
                .filter(this::namesAFailure)
                .limit(1)
                .map(text -> new ProposedFinding(criterion.code(), StandardSeverity.MAJOR,
                        "The inspector wrote something that names a failure", List.of(new Quote(text))));
    }

    private boolean namesAFailure(String text) {
        String lowerCase = text.toLowerCase(Locale.ROOT);
        return FAILURE_CUES.stream().anyMatch(lowerCase::contains);
    }
}
