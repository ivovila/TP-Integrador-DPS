package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El global exige un parcial activo por cada subsistema requerido, del mismo activo, versión y jurisdicción.
 * Cada regla que un parcial no cumple aparece como motivo propio.
 */
public final class AllRequiredSubsystemsPolicy implements GlobalCertificationPolicy {

    private final IneligibilityReason invalidPartialCertificate = StandardIneligibilityReason.INVALID_PARTIAL_CERTIFICATE;
    private final Requirements<Set<SubsystemCode>> schemaRequirements = new Requirements<>(List.of(
            new Requirement<>(requiredSubsystems -> !requiredSubsystems.isEmpty(),
                    StandardIneligibilityReason.MISSING_SUBSYSTEM,
                    requiredSubsystems -> "Schema has no required subsystems")));

    @Override
    public String code() {
        return "ALL-REQUIRED-SUBSYSTEMS";
    }

    @Override
    public CertificationDecision evaluate(GlobalAssessmentContext context) {
        Requirements<Certificate> backingRequirements = backingRequirements(context);
        List<Ineligibility> reasons = new ArrayList<>(schemaRequirements.unmetBy(context.requiredSubsystems()));
        context.partials().forEach(partial -> reasons.addAll(backingRequirements.unmetBy(partial)));
        List<Certificate> validPartials = context.partials().stream().filter(backingRequirements::areMetBy).toList();
        reasons.addAll(duplicatedSubsystems(validPartials));
        reasons.addAll(missingSubsystems(context.requiredSubsystems(), validPartials));
        return reasons.isEmpty() ? granted(validPartials, context) : rejected(reasons, context);
    }

    /** Lo que tiene que cumplir cada parcial para respaldar el global de este activo, versión y jurisdicción. */
    private Requirements<Certificate> backingRequirements(GlobalAssessmentContext context) {
        return new Requirements<>(List.of(
                new Requirement<>(partial -> partial.certifies(context.assetCode()), invalidPartialCertificate,
                        partial -> partial.number().value() + " belongs to another asset"),
                new Requirement<>(partial -> partial.isIssuedFor(context.schemaReference()), invalidPartialCertificate,
                        partial -> partial.number().value() + " was issued for another schema version"),
                new Requirement<>(partial -> partial.certifiesOneOf(context.requiredSubsystems()),
                        invalidPartialCertificate,
                        partial -> partial.number().value() + " does not certify a required subsystem"),
                new Requirement<>(partial -> partial.wasDecidedUnder(context.jurisdictionCode()),
                        invalidPartialCertificate,
                        partial -> partial.number().value() + " was decided under another jurisdiction"),
                new Requirement<>(partial -> partial.statusOn(context.issuanceDate(), context.partials()).isActive(),
                        invalidPartialCertificate, partial -> partial.number().value() + " is not active")));
    }

    private CertificationDecision rejected(List<Ineligibility> reasons, GlobalAssessmentContext context) {
        return new RejectedDecision(reasons, code(), context.jurisdictionCode(), context.evaluatedAt(),
                new WholeAssetScope());
    }

    private List<Ineligibility> duplicatedSubsystems(List<Certificate> validPartials) {
        return timesEachSubsystemIsBackedBy(validPartials).entrySet().stream()
                .filter(timesBacked -> timesBacked.getValue() > 1)
                .map(timesBacked -> new Ineligibility(invalidPartialCertificate,
                        "Subsystem " + timesBacked.getKey().value() + " is backed more than once"))
                .toList();
    }

    private List<Ineligibility> missingSubsystems(Set<SubsystemCode> requiredSubsystems, List<Certificate> validPartials) {
        Set<SubsystemCode> backedSubsystems = timesEachSubsystemIsBackedBy(validPartials).keySet();
        return requiredSubsystems.stream()
                .sorted(Comparator.comparing(SubsystemCode::value))
                .filter(subsystemCode -> !backedSubsystems.contains(subsystemCode))
                .map(subsystemCode -> new Ineligibility(StandardIneligibilityReason.MISSING_SUBSYSTEM,
                        "Missing partial for " + subsystemCode.value()))
                .toList();
    }

    private Map<SubsystemCode, Long> timesEachSubsystemIsBackedBy(List<Certificate> partials) {
        return partials.stream()
                .flatMap(partial -> partial.certifiedSubsystems().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /** Rige desde hoy hasta el menor vencimiento de sus parciales y hereda sus condiciones, sin extenderlas. */
    private CertificationDecision granted(List<Certificate> partials, GlobalAssessmentContext context) {
        LocalDate validUntil = Collections.min(partials.stream().map(partial -> partial.validity().validUntil()).toList());
        List<CertificationCondition> conditions = partials.stream()
                .flatMap(partial -> partial.decision().conditions().stream())
                .map(condition -> new CertificationCondition(condition.findingId(), condition.criterionCode(),
                        condition.description(), Collections.min(List.of(condition.dueDate(), validUntil))))
                .distinct()
                .toList();
        return new GrantedDecision(conditions, new ValidityPeriod(context.issuanceDate(), validUntil), code(),
                context.jurisdictionCode(), context.evaluatedAt(), new WholeAssetScope());
    }
}
