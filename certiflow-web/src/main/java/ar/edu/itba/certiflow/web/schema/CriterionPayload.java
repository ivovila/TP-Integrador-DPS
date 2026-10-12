package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.List;

public record CriterionPayload(String code, String text, String severity, RulePayload rule,
                               List<EvidenceRequirementPayload> requiredEvidence) {

    public CriterionPayload {
        RequestFields.require(code, "criterion.code");
        RequestFields.require(text, "criterion.text");
        RequestFields.require(severity, "criterion.severity");
        RequestFields.require(rule, "criterion.rule");
        requiredEvidence = requiredEvidence == null ? List.of() : requiredEvidence;
    }
}
