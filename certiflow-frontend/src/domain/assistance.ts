import type { Severity } from './schema';
import type { Instant, Person } from './shared';

/** Lo que decidió el inspector sobre una sugerencia. Aceptarla solo deja la traza: la respuesta la corrige él. */
export type SuggestionDecision =
  | { status: 'PENDING' }
  | { status: 'ACCEPTED'; decidedBy: Person; decidedAt: Instant }
  | { status: 'DISMISSED'; decidedBy: Person; decidedAt: Instant; reason: string };

export type SuggestionAction = 'acceptSuggestion' | 'dismissSuggestion';

/** Un hallazgo que propuso el asistente y que pasó los guardrails. */
export interface Suggestion {
  position: number;
  criterionCode: string;
  /** La severidad configurada en el criterio: la que tendría el hallazgo. */
  criterionSeverity: Severity;
  /** La que estimó el asistente a partir del texto; puede no coincidir. */
  estimatedSeverity: Severity;
  rationale: string;
  quotes: string[];
  decision: SuggestionDecision;
  actions: SuggestionAction[];
}

/** Una propuesta que frenó un guardrail: no se puede aceptar, pero queda a la vista. */
export interface RejectedProposal {
  criterionCode: string;
  estimatedSeverity: Severity;
  rationale: string;
  quotes: string[];
  reason: string;
}

export interface AssistantReview {
  id: string;
  inspectionId: string;
  promptVersion: string;
  modelId: string;
  requestedBy: Person;
  requestedAt: Instant;
  suggestions: Suggestion[];
  rejectedProposals: RejectedProposal[];
}
