package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;

/** El código que identifica a un criterio dentro de una versión de esquema. */
public record CriterionCode(String value) {

    public CriterionCode {
        if (value.isBlank()) {
            throw new InvalidSchemaException("A criterion needs a code");
        }
    }
}
