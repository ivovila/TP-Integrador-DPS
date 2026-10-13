package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.List;

public final class RequestFields {

    private RequestFields() {
    }

    public static void require(Object value, String field) {
        if (value == null) {
            throw new RequiredValueMissingException("Missing field " + field);
        }
    }

    public static <T> List<T> requireElements(List<T> values, String field) {
        if (values == null) {
            return List.of();
        }
        if (values.contains(null)) {
            throw new RequiredValueMissingException("Field " + field + " contains a null element");
        }
        return values;
    }
}
