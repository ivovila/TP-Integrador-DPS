package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Representación pública de alcance, base y decisión. Alcance y base llevan un campo {@code type}
 * para que el cliente distinga cada variante sin depender de la forma de los records internos.
 */
public final class CertificationPayloads {

    private CertificationPayloads() {
    }

    public record ScopePayload(String type, String subsystemCode) {
        public static ScopePayload of(CertificationScope scope) {
            return scope.describedBy(new ScopePayloadDescriber());
        }
    }

    public record BasisPayload(String type, String inspectionId, Instant inspectionClosedAt,
                               List<String> partialCertificateNumbers, String globalPolicy) {
        public static BasisPayload of(CertificateBasis basis) {
            return basis.describedBy(new BasisPayloadDescriber());
        }
    }

    public record ReasonPayload(String reason, String detail) {
    }

    public record ConditionPayload(String findingId, String criterionCode, String description, LocalDate dueDate) {
    }

    public record DecisionPayload(String outcome, boolean eligible, List<ReasonPayload> reasons, List<ConditionPayload> conditions,
                                  LocalDate validFrom, LocalDate validUntil, String policyCode, String jurisdictionCode,
                                  Instant evaluatedAt, ScopePayload scope) {
        public static DecisionPayload of(CertificationDecision decision) {
            return new DecisionPayload(decision.outcome().name(), decision.isEligible(),
                    decision.ineligibilities().stream()
                            .map(ineligibility -> new ReasonPayload(ineligibility.reason().code(), ineligibility.detail())).toList(),
                    decision.conditions().stream()
                            .map(condition -> new ConditionPayload(condition.findingId(), condition.criterionCode(),
                                    condition.description(), condition.dueDate())).toList(),
                    decision.granted().map(granted -> granted.validity().validFrom()).orElse(null),
                    decision.granted().map(granted -> granted.validity().validUntil()).orElse(null),
                    decision.policyCode().value(), decision.jurisdictionCode().value(), decision.evaluatedAt(),
                    ScopePayload.of(decision.scope()));
        }
    }

    public record SchemaReferencePayload(String assetType, int number) {
        public static SchemaReferencePayload of(SchemaReference reference) {
            return new SchemaReferencePayload(reference.assetType().name(), reference.number());
        }
    }
}
