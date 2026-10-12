package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public final class RequestFields {

    private RequestFields() {
    }

    public static void require(Object value, String field) {
        if (value == null) {
            throw new RequiredValueMissingException("Missing field " + field);
        }
    }
}
