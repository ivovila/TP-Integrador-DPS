package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecisionDescriber;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.RejectedDecision;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ReasonPayload;
import java.util.List;

/** Los motivos por los que una decisión no certifica: una otorgada no tiene ninguno. */
final class IneligibilityReasons implements CertificationDecisionDescriber<List<ReasonPayload>> {

    @Override
    public List<ReasonPayload> granted(GrantedDecision decision) {
        return List.of();
    }

    @Override
    public List<ReasonPayload> rejected(RejectedDecision decision) {
        return decision.ineligibilities().stream()
                .map(ineligibility -> new ReasonPayload(ineligibility.reason().code(), ineligibility.detail()))
                .toList();
    }
}
