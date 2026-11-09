package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Un caso del conjunto dorado, tal como está en su JSON: lo que cargó el inspector y lo que una persona experta
 * esperaría que el asistente señale. El texto, la regla y el resultado de cada criterio no están acá: salen del
 * esquema, como en la aplicación.
 */
public record GoldenCase(String id, String kind, String description, String assetType, int schemaVersion,
                         List<GoldenCriterion> criteria, List<ExpectedFinding> expected) {

    public record GoldenCriterion(String code, String answer, List<String> observations, List<GoldenEvidence> evidence) {
    }

    public record GoldenEvidence(String kind, String description) {
    }

    public record ExpectedFinding(String criterion, String severity) {
    }

    public GoldenCase {
        criteria = List.copyOf(criteria);
        expected = List.copyOf(expected);
    }

    public Set<CriterionCode> expectedCriteria() {
        return expected.stream().map(finding -> new CriterionCode(finding.criterion())).collect(Collectors.toSet());
    }

    public Optional<String> expectedSeverityOf(CriterionCode criterion) {
        return expected.stream()
                .filter(finding -> finding.criterion().equals(criterion.value()))
                .map(ExpectedFinding::severity)
                .findFirst();
    }
}
