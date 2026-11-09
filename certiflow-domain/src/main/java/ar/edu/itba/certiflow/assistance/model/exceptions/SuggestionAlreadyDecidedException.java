package ar.edu.itba.certiflow.assistance.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class SuggestionAlreadyDecidedException extends RuleViolationException {

    public SuggestionAlreadyDecidedException(int position) {
        super("Suggestion " + position + " was already decided");
    }
}
