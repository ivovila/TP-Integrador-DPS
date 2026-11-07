package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.Locale;

/**
 * Un fragmento del texto de la inspección en el que el modelo apoya una sugerencia. Se compara literal salvo
 * mayúsculas y espacios: un cambio de formato no la invalida, una paráfrasis sí. Una cita vacía aparecería en
 * cualquier texto, por eso no existe.
 */
public record Quote(String text) {

    public Quote {
        if (text.isBlank()) {
            throw new RequiredValueMissingException("A quote must not be blank");
        }
    }

    public boolean appearsIn(String source) {
        return normalized(source).contains(normalized(text));
    }

    private String normalized(String value) {
        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
