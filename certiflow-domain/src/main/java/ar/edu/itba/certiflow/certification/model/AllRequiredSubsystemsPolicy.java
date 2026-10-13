package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.time.Instant;
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

    private static final IneligibilityReason INVALID = IneligibilityReason.INVALID_PARTIAL_CERTIFICATE;

    @Override
    public String code() { return "ALL-REQUIRED-SUBSYSTEMS"; }

    @Override
    public CertificationDecision evaluate(AssetCode asset, SchemaReference schema, Set<SubsystemCode> required,
            List<Certificate> partials, JurisdictionCode jurisdiction, LocalDate date, Instant evaluatedAt) {
        List<Requirement<Certificate>> backing = List.of(
                Requirement.that(partial -> partial.assetCode().equals(asset), INVALID,
                        partial -> partial.number().value() + " belongs to another asset"),
                Requirement.that(partial -> partial.schemaReference().equals(schema), INVALID,
                        partial -> partial.number().value() + " was issued for another schema version"),
                Requirement.that(partial -> partial.scope().subsystemCode().filter(required::contains).isPresent(), INVALID,
                        partial -> partial.number().value() + " does not certify a required subsystem"),
                Requirement.that(partial -> partial.decision().jurisdictionCode().equals(jurisdiction), INVALID,
                        partial -> partial.number().value() + " was decided under another jurisdiction"),
                Requirement.that(partial -> partial.statusOn(date) == CertificateStatus.ACTIVE, INVALID,
                        partial -> partial.number().value() + " is not active"));

        List<Ineligibility> reasons = new ArrayList<>(Requirement.unmetBy(List.of(
                Requirement.<Set<SubsystemCode>>that(codes -> !codes.isEmpty(), IneligibilityReason.MISSING_SUBSYSTEM,
                        codes -> "Schema has no required subsystems")), required));
        partials.forEach(partial -> reasons.addAll(Requirement.unmetBy(backing, partial)));
        List<Certificate> valid = partials.stream().filter(partial -> Requirement.unmetBy(backing, partial).isEmpty()).toList();
        reasons.addAll(duplicatedSubsystems(valid));
        reasons.addAll(missingSubsystems(required, valid));

        CertificationDecision rejection = new CertificationDecision(CertificationOutcome.REJECTED, reasons, List.of(),
                Optional.empty(), code(), jurisdiction, evaluatedAt, new CertificationScope.WholeAsset());
        return reasons.isEmpty() ? granted(valid, jurisdiction, date, evaluatedAt) : rejection;
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
                .map(code -> new Ineligibility(IneligibilityReason.MISSING_SUBSYSTEM, "Missing partial for " + code.value()))
                .toList();
    }

    private static Map<SubsystemCode, Long> subsystemsOf(List<Certificate> partials) {
        return partials.stream().flatMap(partial -> partial.scope().subsystemCode().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /** Rige desde hoy hasta el menor vencimiento de sus parciales y hereda sus condiciones, sin extenderlas. */
    private CertificationDecision granted(List<Certificate> partials, JurisdictionCode jurisdiction, LocalDate date, Instant evaluatedAt) {
        LocalDate until = partials.stream().map(partial -> partial.validity().validUntil()).min(LocalDate::compareTo).orElseThrow();
        List<CertificationCondition> conditions = partials.stream().flatMap(partial -> partial.decision().conditions().stream())
                .map(condition -> new CertificationCondition(condition.findingId(), condition.criterionCode(), condition.description(),
                        Collections.min(List.of(condition.dueDate(), until))))
                .distinct().toList();
        return CertificationDecision.granted(conditions, new ValidityPeriod(date, until), code(), jurisdiction, evaluatedAt,
                new CertificationScope.WholeAsset());
    }
}
