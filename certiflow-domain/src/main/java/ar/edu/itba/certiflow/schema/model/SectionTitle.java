package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;

/** El título que identifica a una sección dentro de una versión de esquema. */
public record SectionTitle(String value) {

    public SectionTitle {
        if (value.isBlank()) {
            throw new InvalidSchemaException("A section needs a title");
        }
    }
}
