package ar.edu.itba.certiflow.harness;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.Suggestion;
import ar.edu.itba.certiflow.harness.golden.GoldenCase;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mide una revisión contra lo esperado (spec §7.2), sobre las sugerencias que pasaron los guardrails. Cuenta
 * criterios, no sugerencias: proponer dos veces el mismo criterio no es acertar dos veces. Si el modelo repite un
 * criterio, la severidad que cuenta es la de su primera sugerencia.
 */
public final class Scoring {

    public RunResult score(int run, GoldenCase goldenCase, AssistantReview review) {
        Map<CriterionCode, ProposedFinding> suggested = review.suggestions().stream().map(Suggestion::proposal)
                .collect(Collectors.toMap(ProposedFinding::criterion, Function.identity(), (first, second) -> first,
                        LinkedHashMap::new));
        Set<CriterionCode> expected = goldenCase.expectedCriteria();
        int correct = (int) suggested.keySet().stream().filter(expected::contains).count();
        int severityHits = (int) suggested.values().stream()
                .filter(proposal -> goldenCase.expectedSeverityOf(proposal.criterion())
                        .filter(severity -> severity.equals(proposal.estimatedSeverity().code()))
                        .isPresent())
                .count();
        int missed = (int) expected.stream().filter(criterion -> !suggested.containsKey(criterion)).count();
        return new RunResult(run, correct, severityHits, suggested.size() - correct, missed,
                review.suggestions().stream().map(suggestion -> proposal(suggestion.proposal())).toList(),
                review.rejectedProposals().stream()
                        .map(rejected -> new RunResult.Rejection(proposal(rejected.proposal()), rejected.reason().name()))
                        .toList());
    }

    private RunResult.Proposal proposal(ProposedFinding finding) {
        return new RunResult.Proposal(finding.criterion().value(), finding.estimatedSeverity().code(), finding.rationale(),
                finding.quotes().stream().map(Quote::text).toList());
    }
}
