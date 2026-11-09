package ar.edu.itba.certiflow.web.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import ar.edu.itba.certiflow.schema.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SchemaPayloadsTest {
    record PrefixRule(String prefix) implements ApprovalRule<OptionAnswer> {
        public boolean isSatisfiedBy(OptionAnswer answer) { return answer.option().startsWith(prefix); }
        public OptionAnswer answerFrom(RawAnswer raw) { return new OptionAnswer(raw.text()); }
        public <T> T describedBy(RuleDescriber<T> describer) { return describer.text("prefix", prefix).ofKind("prefix"); }
    }

    @Test
    void customRuleParametersAndSeverityArePublishedWithoutConcreteTypeCasts() {
        Severity severity = new Severity() {
            public String code() { return "CUSTOM"; }
            public String label() { return "Custom"; }
            public Outcome outcomeWhenUnmet() { return Outcome.OBSERVED; }
        };
        Criterion<?> criterion = new Criterion<>(new CriterionCode("C-1"), "Prefix", new PrefixRule("OK"), severity, List.of());
        CriterionPayload payload = SchemaPayloads.payload(criterion);
        JsonMapper json = JsonMapper.builder().build();
        var document = json.readTree(json.writeValueAsString(payload));
        assertEquals("CUSTOM", document.get("severity").asString());
        assertEquals("prefix", document.get("rule").get("type").asString());
        assertEquals("OK", document.get("rule").get("prefix").asString());
        assertEquals("OK", json.readValue(document.get("rule").toString(), RulePayload.class).text("prefix"));
    }
}
