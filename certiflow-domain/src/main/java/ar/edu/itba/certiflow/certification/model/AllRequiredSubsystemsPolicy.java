package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public final class AllRequiredSubsystemsPolicy implements GlobalCertificationPolicy {
    @Override
    public String code() { return "ALL-REQUIRED-SUBSYSTEMS"; }

    @Override
    public CertificationDecision evaluate(AssetCode asset, SchemaReference schema, Set<SubsystemCode> required,
            List<Certificate> partials, JurisdictionCode jurisdiction, LocalDate date, Instant evaluatedAt) {
        List<Ineligibility> reasons = new ArrayList<>();
        Set<SubsystemCode> seen = new HashSet<>();
        if (required.isEmpty()) {
            reasons.add(new Ineligibility(IneligibilityReason.MISSING_SUBSYSTEM, "Schema has no required subsystems"));
        }
        for (Certificate partial : partials) {
            if (!partial.assetCode().equals(asset) || !partial.schemaReference().equals(schema)
                    || !(partial.scope() instanceof CertificationScope.SubsystemScope subsystem)
                    || !required.contains(subsystem.code()) || !seen.add(subsystem.code())
                    || !partial.decision().jurisdictionCode().equals(jurisdiction)
                    || partial.statusOn(date) != CertificateStatus.ACTIVE) {
                reasons.add(new Ineligibility(IneligibilityReason.INVALID_PARTIAL_CERTIFICATE, "Invalid backing " + partial.number().value()));
            }
        }
        required.stream().sorted(Comparator.comparing(SubsystemCode::value)).filter(code -> !seen.contains(code))
                .forEach(code -> reasons.add(new Ineligibility(IneligibilityReason.MISSING_SUBSYSTEM, "Missing partial for " + code.value())));
        if (!reasons.isEmpty()) {
            return new CertificationDecision(CertificationOutcome.REJECTED, reasons, List.of(), Optional.empty(), code(),
                    jurisdiction, evaluatedAt, new CertificationScope.WholeAsset());
        }
        var until = partials.stream().map(partial -> partial.validity().validUntil()).min(LocalDate::compareTo).orElseThrow();
        var conditions = partials.stream().flatMap(partial -> partial.decision().conditions().stream())
                .map(condition -> new CertificationCondition(condition.findingId(), condition.criterionCode(), condition.description(),
                        condition.dueDate().isAfter(until) ? until : condition.dueDate())).distinct().toList();
        return new CertificationDecision(conditions.isEmpty() ? CertificationOutcome.APPROVED : CertificationOutcome.CONDITIONAL,
                List.of(), conditions, Optional.of(new ValidityPeriod(date, until)), code(),
                jurisdiction, evaluatedAt, new CertificationScope.WholeAsset());
    }
}
