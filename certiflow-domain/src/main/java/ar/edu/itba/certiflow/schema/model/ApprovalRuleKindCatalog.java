package ar.edu.itba.certiflow.schema.model;

import java.util.List;
import java.util.Optional;

/** Los tipos de regla que el sistema conoce. Quien lee una regla guardada o recibida busca su tipo acá. */
public final class ApprovalRuleKindCatalog {

    private final List<ApprovalRuleKind> knownRuleKinds;

    public ApprovalRuleKindCatalog(List<ApprovalRuleKind> knownRuleKinds) {
        this.knownRuleKinds = List.copyOf(knownRuleKinds);
    }

    public Optional<ApprovalRuleKind> named(String ruleKindName) {
        return knownRuleKinds.stream().filter(ruleKind -> ruleKind.isNamed(ruleKindName)).findFirst();
    }
}
