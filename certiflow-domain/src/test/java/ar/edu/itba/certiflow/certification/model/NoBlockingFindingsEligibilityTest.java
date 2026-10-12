package ar.edu.itba.certiflow.certification.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NoBlockingFindingsEligibilityTest {

    private static final Optional<Instant> CLOSED = Optional.of(Instant.parse("2026-03-10T10:00:00Z"));

    private static InspectionResult result(Optional<Instant> closedAt, boolean wholeSchema, OpenNonConformity... open) {
        return new InspectionResult(new InspectionId("I-1"), new AssetCode("EXT-0042"), closedAt, wholeSchema, List.of(open));
    }

    static Stream<Arguments> results() {
        return Stream.of(
                Arguments.of(result(CLOSED, true), List.of()),
                Arguments.of(result(CLOSED, true, new OpenNonConformity("EXT-03", StandardSeverity.MINOR)), List.of()),
                Arguments.of(result(CLOSED, true, new OpenNonConformity("EXT-01", StandardSeverity.MAJOR)),
                        List.of(IneligibilityReason.BLOCKING_FINDINGS)),
                Arguments.of(result(CLOSED, true, new OpenNonConformity("EXT-02", StandardSeverity.CRITICAL)),
                        List.of(IneligibilityReason.BLOCKING_FINDINGS)),
                Arguments.of(result(Optional.empty(), true), List.of(IneligibilityReason.INSPECTION_NOT_CLOSED)),
                Arguments.of(result(CLOSED, false), List.of(IneligibilityReason.PARTIAL_INSPECTION)),
                Arguments.of(result(Optional.empty(), false, new OpenNonConformity("EXT-01", StandardSeverity.MAJOR)),
                        List.of(IneligibilityReason.INSPECTION_NOT_CLOSED, IneligibilityReason.PARTIAL_INSPECTION,
                                IneligibilityReason.BLOCKING_FINDINGS)));
    }

    @ParameterizedTest
    @MethodSource("results")
    void decisionListsEveryReasonTheResultCannotBeCertified(InspectionResult result, List<IneligibilityReason> expected) {
        CertificationDecision decision = new NoBlockingFindingsEligibility().evaluate(result);

        assertEquals(expected, decision.ineligibilities().stream().map(Ineligibility::reason).toList());
    }
}
