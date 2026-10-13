package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Una respuesta guardada como texto con la etiqueta de su tipo de valor. Un tipo de valor nuevo es un codec más. */
record AnswerColumns(String type, String value) {

    private record ValueCodec<T>(String tag, Class<T> valueType, Function<T, String> encode, Function<String, T> decode) {

        Optional<AnswerColumns> write(RawAnswer raw) {
            return raw.as(valueType).map(value -> new AnswerColumns(tag, encode.apply(value)));
        }

        RawAnswer read(String text) {
            return new RawAnswer(decode.apply(text));
        }
    }

    private static final List<ValueCodec<?>> CODECS = List.of(
            new ValueCodec<>("NUMBER", BigDecimal.class, BigDecimal::toPlainString, BigDecimal::new),
            new ValueCodec<>("YES_NO", Boolean.class, String::valueOf, Boolean::parseBoolean),
            new ValueCodec<>("OPTION", String.class, Function.identity(), Function.identity()));

    static AnswerColumns of(RawAnswer raw) {
        return CODECS.stream().flatMap(codec -> codec.write(raw).stream()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No column format for " + raw.value()));
    }

    RawAnswer toRaw() {
        return CODECS.stream().filter(codec -> codec.tag().equals(type)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown answer type " + type))
                .read(value);
    }
}
