package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.schema.model.RuleDescriber;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Una regla escrita para que la lea el asistente: su tipo y sus parámetros con nombre, sean los que sean. Un tipo
 * de regla nuevo se escribe sin tocar esta clase. Las opciones van ordenadas para que la misma regla se lea igual.
 */
final class RuleInWords implements RuleDescriber<String> {

    private final List<String> parameters = new ArrayList<>();

    @Override
    public RuleDescriber<String> text(String parameterName, String value) {
        parameters.add(parameterName + ": " + value);
        return this;
    }

    @Override
    public RuleDescriber<String> decimal(String parameterName, BigDecimal value) {
        parameters.add(parameterName + ": " + value.toPlainString());
        return this;
    }

    @Override
    public RuleDescriber<String> texts(String parameterName, Set<String> values) {
        parameters.add(parameterName + ": " + String.join(", ", new TreeSet<>(values)));
        return this;
    }

    @Override
    public String ofKind(String ruleKindName) {
        return ruleKindName + " (" + String.join("; ", parameters) + ")";
    }
}
