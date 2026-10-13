package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Representación pública de alcance, base y decisión. Los tipos sellados del dominio llevan un campo {@code type}
 * para que el cliente distinga cada variante sin depender de la forma de los records internos.
 */
public final class CertificationPayloads {

    private CertificationPayloads() {
    }

    public record ScopePayload(String type, String subsystemCode) {
        public static ScopePayload of(CertificationScope scope) {
            return switch (scope) {
                case CertificationScope.WholeAsset ignored -> new ScopePayload("WHOLE_ASSET", null);
                case CertificationScope.SubsystemScope subsystem -> new ScopePayload("SUBSYSTEM", subsystem.code().value());
            };
        }
    }

    public record BasisPayload(String type, String inspectionId, Instant inspectionClosedAt,
                               List<String> partialCertificateNumbers, String globalPolicy) {
        public static BasisPayload of(CertificateBasis basis) {
            return switch (basis) {
                case CertificateBasis.InspectionBasis inspection -> new BasisPayload("INSPECTION",
                        inspection.inspection().id().value(), inspection.inspection().closedAt(), List.of(), null);
                case CertificateBasis.PartialCertificatesBasis partials -> new BasisPayload("PARTIAL_CERTIFICATES", null,
                        partials.latestInspectionClosedAt(),
                        partials.partials().stream().map(number -> number.value()).toList(), partials.globalPolicy());
            };
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
                            .map(ineligibility -> new ReasonPayload(ineligibility.reason().name(), ineligibility.detail())).toList(),
                    decision.conditions().stream()
                            .map(condition -> new ConditionPayload(condition.findingId(), condition.criterionCode(),
                                    condition.description(), condition.dueDate())).toList(),
                    decision.validity().map(validity -> validity.validFrom()).orElse(null),
                    decision.validity().map(validity -> validity.validUntil()).orElse(null),
                    decision.policyCode(), decision.jurisdictionCode().value(), decision.evaluatedAt(),
                    ScopePayload.of(decision.scope()));
        }
    }

    public record SchemaReferencePayload(String assetType, int number) {
        public static SchemaReferencePayload of(SchemaReference reference) {
            return new SchemaReferencePayload(reference.assetType().name(), reference.number());
        }
    }
}
