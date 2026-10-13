package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;

public final class DecisionJson {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private DecisionJson() { }

    record Document(CertificationOutcome outcome, List<Ineligibility> ineligibilities,
            List<CertificationCondition> conditions, Optional<ValidityPeriod> validity,
            String policyCode, JurisdictionCode jurisdictionCode,
            Instant evaluatedAt, String subsystemCode) { }

    public static String write(CertificationDecision value) {
        String code = value.scope() instanceof CertificationScope.SubsystemScope subsystem
                ? subsystem.code().value() : null;
        return MAPPER.writeValueAsString(new Document(value.outcome(), value.ineligibilities(),
                value.conditions(), value.validity(), value.policyCode(),
                value.jurisdictionCode(), value.evaluatedAt(), code));
    }

    public static CertificationDecision read(String json) {
        var value = MAPPER.readValue(json, Document.class);
        CertificationScope scope = value.subsystemCode() == null ? new CertificationScope.WholeAsset()
                : new CertificationScope.SubsystemScope(new SubsystemCode(value.subsystemCode()));
        return new CertificationDecision(value.outcome(), value.ineligibilities(), value.conditions(),
                value.validity(), value.policyCode(), value.jurisdictionCode(), value.evaluatedAt(), scope);
    }
}
