package ar.edu.itba.certiflow.infrastructure.assistance;

import ar.edu.itba.certiflow.assistance.model.PromptVersion;

/** La plantilla que se manda al modelo, con la versión que queda registrada en cada revisión. */
public record ActivePrompt(PromptVersion version, String template) {
}
