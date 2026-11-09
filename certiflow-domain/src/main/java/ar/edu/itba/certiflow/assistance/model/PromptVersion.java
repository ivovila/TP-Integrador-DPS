package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

/** La versión del prompt con que se pidió una revisión: cambiar el prompt es publicar otra, nunca editar una. */
public record PromptVersion(String value) {

    public PromptVersion {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A prompt version must not be blank");
        }
    }
}
