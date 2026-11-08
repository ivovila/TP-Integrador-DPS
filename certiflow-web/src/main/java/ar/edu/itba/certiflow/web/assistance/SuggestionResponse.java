package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.Suggestion;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "suggestions", itemRelation = "suggestion")
public class SuggestionResponse extends RepresentationModel<SuggestionResponse> {

    public final int position;
    public final String criterionCode;
    public final String criterionSeverity;
    public final String estimatedSeverity;
    public final String rationale;
    public final List<String> quotes;
    public final DecisionResponse decision;

    SuggestionResponse(Suggestion suggestion, String criterionSeverity) {
        this.position = suggestion.position();
        this.criterionCode = suggestion.proposal().criterion().value();
        this.criterionSeverity = criterionSeverity;
        this.estimatedSeverity = suggestion.proposal().estimatedSeverity().code();
        this.rationale = suggestion.proposal().rationale();
        this.quotes = suggestion.proposal().quotes().stream().map(Quote::text).toList();
        this.decision = suggestion.decision().describedBy(new DecisionResponse.Describer());
    }
}
