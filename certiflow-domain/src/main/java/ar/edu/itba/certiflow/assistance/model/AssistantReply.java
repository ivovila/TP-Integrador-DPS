package ar.edu.itba.certiflow.assistance.model;

import java.util.List;

/** La respuesta del asistente, con la versión de prompt y el modelo que la produjeron. */
public record AssistantReply(PromptVersion promptVersion, ModelId modelId, List<ProposedFinding> proposals) {

    public AssistantReply {
        proposals = List.copyOf(proposals);
    }
}
