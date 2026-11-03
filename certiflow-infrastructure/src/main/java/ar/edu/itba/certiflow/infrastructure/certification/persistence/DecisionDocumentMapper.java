package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.CertificationCondition;
import ar.edu.itba.certiflow.certification.model.CertificationOutcome;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.certification.model.PolicyReference;
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
                            List<CertificationCondition> conditions, ValidityPeriod validity, String policyCode, int policyVersion,
                            JurisdictionCode jurisdictionCode, Instant evaluatedAt, String subsystemCode) {
    }

    private final StoredJson storedJson;

    public DecisionDocumentMapper(StoredJson storedJson) {
        this.storedJson = storedJson;
    }

    public String write(GrantedDecision decision) {
        return storedJson.write(new DecisionDocument(decision.outcome(), List.of(), decision.conditions(),
                decision.validity(), decision.policy().code().value(), decision.policy().version(), decision.jurisdictionCode(), decision.evaluatedAt(),
                decision.scope().describedBy(new ScopeSubsystemColumn()).orElse(null)));
    }

    public GrantedDecision read(String storedDecision) {
        DecisionDocument document = storedJson.read(storedDecision, DecisionDocument.class);
        return new GrantedDecision(document.conditions(), document.validity(), new PolicyReference(new PolicyCode(document.policyCode()), document.policyVersion()),
                document.jurisdictionCode(), document.evaluatedAt(), new StoredScope(document.subsystemCode()).scope());
    }
}
