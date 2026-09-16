package ar.edu.itba.certiflow.domain;

import java.util.List;

/** All findings, regardless of severity, require a verified and closed correction. */
final class CertificationEligibility {
    private CertificationEligibility() {}

    static void requireEligible(Inspection inspection, List<CorrectiveAction> actions) {
        Checks.require(inspection.state() == Inspection.State.CLOSED, "Inspection must be closed");
        Checks.require(
                inspection.evaluations().size()
                                == inspection.version().orElseThrow().criteria().size()
                        && inspection.evaluations().values().stream()
                                .noneMatch(e -> e.outcome() == Outcome.INCOMPLETE),
                "Complete evaluation required");
        for (Finding finding : inspection.findings()) {
            Checks.require(
                    actions.stream()
                            .anyMatch(
                                    a ->
                                            a.finding().equals(finding)
                                                    && a.state() == CorrectiveAction.State.CLOSED),
                    "Unresolved finding: " + finding.criterionCode());
        }
    }
}
