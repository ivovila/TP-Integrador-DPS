package ar.edu.itba.certiflow.schema.model;

import java.util.Set;

/** Quien necesita escribir una regla (para guardarla o publicarla) recibe lo que cada tipo de regla tiene. */
public interface ApprovalRuleDescriber<T> {

    T numericRange(Unit unit, Range range);

    T yesNo(YesNoAnswer expectedAnswer);

    T optionInList(Set<String> acceptedOptions);
}
