package ar.edu.itba.certiflow.assistance.model;

/** Una propuesta que un guardrail detuvo: queda registrada con su motivo, nunca se muestra ni se puede aceptar. */
public record RejectedProposal(ProposedFinding proposal, GuardrailRejection reason) {
}
