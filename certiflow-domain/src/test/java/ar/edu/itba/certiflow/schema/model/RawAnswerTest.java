package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.schema.model.exceptions.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RawAnswerTest {

    private static final ApprovalRule<NumericAnswer> NUMERIC =
            new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6"), new BigDecimal("8")));
    private static final ApprovalRule<YesNoAnswer> YES_NO = new YesNoRule(YesNoAnswer.YES);
    private static final ApprovalRule<OptionAnswer> OPTION = new OptionInListRule(Set.of("VISIBLE"));

    static Stream<Arguments> acceptedAnswers() {
        return Stream.of(
                Arguments.of(NUMERIC, new RawAnswer("7.00"), new NumericAnswer(new BigDecimal("7.00"))),
                Arguments.of(YES_NO, new RawAnswer("true"), YesNoAnswer.YES),
                Arguments.of(YES_NO, new RawAnswer("false"), YesNoAnswer.NO),
                Arguments.of(OPTION, new RawAnswer("VISIBLE"), new OptionAnswer("VISIBLE")));
    }

    static Stream<Arguments> mismatchedAnswers() {
        return Stream.of(
                Arguments.of(NUMERIC, new RawAnswer("true")),
                Arguments.of(NUMERIC, new RawAnswer("seven")),
                Arguments.of(NUMERIC, new RawAnswer("")),
                Arguments.of(YES_NO, new RawAnswer("1")),
                Arguments.of(YES_NO, new RawAnswer("YES")),
                Arguments.of(YES_NO, new RawAnswer("TRUE")));
    }

    @ParameterizedTest
    @MethodSource("acceptedAnswers")
    void eachRuleTurnsTheTextItAcceptsIntoItsTypedAnswer(ApprovalRule<?> rule, RawAnswer raw, Answer expected) {
        assertEquals(expected, rule.answerFrom(raw));
    }

    @ParameterizedTest
    @MethodSource("mismatchedAnswers")
    void eachRuleRejectsATextItCannotRead(ApprovalRule<?> rule, RawAnswer raw) {
        assertThrows(AnswerTypeMismatchException.class, () -> rule.answerFrom(raw));
    }

    @ParameterizedTest
    @MethodSource("acceptedAnswers")
    void aTypedAnswerGoesBackToTheTextItCameFrom(ApprovalRule<?> rule, RawAnswer raw, Answer typed) {
        assertEquals(raw, new RawAnswer(typed.describedBy(new AnswerText())));
    }

    @Test
    void aNumberSentAsTextIsReadAsANumber() {
        assertEquals(new NumericAnswer(new BigDecimal("7")), NUMERIC.answerFrom(new RawAnswer("7")));
    }

    @Test
    void anyTextIsAnOptionAnswerAndTheRuleDecidesWhetherItIsAccepted() {
        OptionAnswer numberAsOption = OPTION.answerFrom(new RawAnswer("1"));

        assertEquals(new OptionAnswer("1"), numberAsOption);
        assertEquals(false, OPTION.isSatisfiedBy(numberAsOption));
    }

    @Test
    void aVersionFindsItsCriterionByCodeAndRejectsAnUnknownOne() {
        Criterion<YesNoAnswer> seal = new Criterion<>("EXT-02", "Seal intact", YES_NO, StandardSeverity.CRITICAL, List.of());
        SchemaVersion version = new SchemaVersion(1, List.of(new Section("Seal", List.of(seal))), Instant.EPOCH, Instant.EPOCH, Set.of());

        assertEquals(seal, version.criterion("EXT-02"));
        assertThrows(CriterionNotInSchemaException.class, () -> version.criterion("EXT-99"));
    }
}
