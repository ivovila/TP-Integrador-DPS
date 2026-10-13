package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.CertificationCondition;
import ar.edu.itba.certiflow.certification.model.CertificationOutcome;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DecisionDocumentMapper {

    record ReasonDocument(String reason, String detail) {
    }

    record DecisionDocument(CertificationOutcome outcome, List<ReasonDocument> ineligibilities,
                            List<CertificationCondition> conditions, ValidityPeriod validity, String policyCode,
                            JurisdictionCode jurisdictionCode, Instant evaluatedAt, String subsystemCode) {
    }

    private final StoredJson storedJson;

    public DecisionDocumentMapper(StoredJson storedJson) {
        this.storedJson = storedJson;
    }

    public String write(GrantedDecision decision) {
        List<ReasonDocument> ineligibilities = decision.ineligibilities().stream()
                .map(ineligibility -> new ReasonDocument(ineligibility.reason().code(), ineligibility.detail()))
                .toList();
        return storedJson.write(new DecisionDocument(decision.outcome(), ineligibilities, decision.conditions(),
                decision.validity(), decision.policyCode(), decision.jurisdictionCode(), decision.evaluatedAt(),
                decision.scope().describedBy(new ScopeSubsystemColumn()).orElse(null)));
    }

    public GrantedDecision read(String storedDecision) {
        DecisionDocument document = storedJson.read(storedDecision, DecisionDocument.class);
        return new GrantedDecision(document.conditions(), document.validity(), document.policyCode(),
                document.jurisdictionCode(), document.evaluatedAt(), new StoredScope(document.subsystemCode()).scope());
    }
}
