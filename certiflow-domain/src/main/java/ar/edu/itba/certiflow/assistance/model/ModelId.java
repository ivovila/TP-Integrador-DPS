package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

/** El modelo que respondió una revisión, tal como lo nombra su proveedor. */
public record ModelId(String value) {

    public ModelId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A model id must not be blank");
        }
    }
}
