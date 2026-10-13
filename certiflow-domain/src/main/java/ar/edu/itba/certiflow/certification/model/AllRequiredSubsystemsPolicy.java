package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El global exige un parcial activo por cada subsistema requerido, del mismo activo, versión y jurisdicción.
 * Cada regla que un parcial no cumple aparece como motivo propio.
 */
public final class AllRequiredSubsystemsPolicy implements GlobalCertificationPolicy {

    private static final IneligibilityReason INVALID = StandardIneligibilityReason.INVALID_PARTIAL_CERTIFICATE;

    private static final List<Requirement<Set<SubsystemCode>>> SCHEMA_REQUIREMENTS = List.of(
            Requirement.that(codes -> !codes.isEmpty(), StandardIneligibilityReason.MISSING_SUBSYSTEM,
                    codes -> "Schema has no required subsystems"));

    @Override
    public String code() { return "ALL-REQUIRED-SUBSYSTEMS"; }

    @Override
    public CertificationDecision evaluate(GlobalAssessmentContext context) {
        List<Requirement<Certificate>> backing = backingRequirements(context);
        List<Ineligibility> reasons = new ArrayList<>(Requirement.unmetBy(SCHEMA_REQUIREMENTS, context.requiredSubsystems()));
        context.partials().forEach(partial -> reasons.addAll(Requirement.unmetBy(backing, partial)));
        List<Certificate> valid = context.partials().stream()
                .filter(partial -> Requirement.unmetBy(backing, partial).isEmpty())
                .toList();
        reasons.addAll(duplicatedSubsystems(valid));
        reasons.addAll(missingSubsystems(context.requiredSubsystems(), valid));
        return reasons.isEmpty() ? granted(valid, context) : rejected(reasons, context);
    }

    /** Lo que tiene que cumplir cada parcial para respaldar el global de este activo, versión y jurisdicción. */
    private static List<Requirement<Certificate>> backingRequirements(GlobalAssessmentContext context) {
        return List.of(
                Requirement.that(partial -> partial.assetCode().equals(context.assetCode()), INVALID,
                        partial -> partial.number().value() + " belongs to another asset"),
                Requirement.that(partial -> partial.schemaReference().equals(context.schemaReference()), INVALID,
                        partial -> partial.number().value() + " was issued for another schema version"),
                Requirement.that(partial -> partial.scope().subsystemCode().filter(context.requiredSubsystems()::contains).isPresent(),
                        INVALID, partial -> partial.number().value() + " does not certify a required subsystem"),
                Requirement.that(partial -> partial.decision().jurisdictionCode().equals(context.jurisdictionCode()), INVALID,
                        partial -> partial.number().value() + " was decided under another jurisdiction"),
                Requirement.that(partial -> partial.statusOn(context.issuanceDate(), context.partials()) == CertificateStatus.ACTIVE,
                        INVALID, partial -> partial.number().value() + " is not active"));
    }

    private CertificationDecision rejected(List<Ineligibility> reasons, GlobalAssessmentContext context) {
        return new CertificationDecision(CertificationOutcome.REJECTED, reasons, List.of(), Optional.empty(), code(),
                context.jurisdictionCode(), context.evaluatedAt(), new CertificationScope.WholeAsset());
    }

    private static List<Ineligibility> duplicatedSubsystems(List<Certificate> valid) {
        return subsystemsOf(valid).entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> new Ineligibility(INVALID, "Subsystem " + entry.getKey().value() + " is backed more than once"))
                .toList();
    }

    private static List<Ineligibility> missingSubsystems(Set<SubsystemCode> required, List<Certificate> valid) {
        Set<SubsystemCode> covered = subsystemsOf(valid).keySet();
        return required.stream().sorted(Comparator.comparing(SubsystemCode::value))
                .filter(code -> !covered.contains(code))
                .map(code -> new Ineligibility(StandardIneligibilityReason.MISSING_SUBSYSTEM, "Missing partial for " + code.value()))
                .toList();
    }

    private static Map<SubsystemCode, Long> subsystemsOf(List<Certificate> partials) {
        return partials.stream().flatMap(partial -> partial.scope().subsystemCode().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /** Rige desde hoy hasta el menor vencimiento de sus parciales y hereda sus condiciones, sin extenderlas. */
    private CertificationDecision granted(List<Certificate> partials, GlobalAssessmentContext context) {
        LocalDate until = partials.stream().map(partial -> partial.validity().validUntil()).min(LocalDate::compareTo).orElseThrow();
        List<CertificationCondition> conditions = partials.stream().flatMap(partial -> partial.decision().conditions().stream())
                .map(condition -> new CertificationCondition(condition.findingId(), condition.criterionCode(), condition.description(),
                        Collections.min(List.of(condition.dueDate(), until))))
                .distinct().toList();
        return CertificationDecision.granted(conditions, new ValidityPeriod(context.issuanceDate(), until), code(), context.jurisdictionCode(), context.evaluatedAt(),
                new CertificationScope.WholeAsset());
    }
}
