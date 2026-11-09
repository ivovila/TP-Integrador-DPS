import type { AssistantReview, RejectedProposal, Suggestion, SuggestionDecision } from '../../domain/assistance';
import type { Vocabulary } from '../../domain/schema';
import { actionsOfferedBy, suggestionActionByRel } from './actions';
import { severityNamed } from './schemaMapping';
import type { WireAssistantReview, WireRejectedProposal, WireSuggestion, WireSuggestionDecision } from './wire';

function decisionOf(wire: WireSuggestionDecision): SuggestionDecision {
  if (wire.status === 'PENDING') {
    return { status: 'PENDING' };
  }
  const decided = { decidedBy: wire.decidedBy!, decidedAt: wire.decidedAt! };
  return wire.status === 'ACCEPTED' ? { status: 'ACCEPTED', ...decided } : { status: 'DISMISSED', ...decided, reason: wire.reason! };
}

function suggestionOf(wire: WireSuggestion, vocabulary: Vocabulary): Suggestion {
  return {
    position: wire.position,
    criterionCode: wire.criterionCode,
    criterionSeverity: severityNamed(vocabulary, wire.criterionSeverity),
    estimatedSeverity: severityNamed(vocabulary, wire.estimatedSeverity),
    rationale: wire.rationale,
    quotes: wire.quotes,
    decision: decisionOf(wire.decision),
    actions: actionsOfferedBy(wire, suggestionActionByRel),
  };
}

function rejectedProposalOf(wire: WireRejectedProposal, vocabulary: Vocabulary): RejectedProposal {
  return {
    criterionCode: wire.criterionCode,
    estimatedSeverity: severityNamed(vocabulary, wire.estimatedSeverity),
    rationale: wire.rationale,
    quotes: wire.quotes,
    reason: wire.reason,
  };
}

export function assistantReviewOf(wire: WireAssistantReview, vocabulary: Vocabulary): AssistantReview {
  return {
    id: wire.id,
    inspectionId: wire.inspectionId,
    promptVersion: wire.promptVersion,
    modelId: wire.modelId,
    requestedBy: wire.requestedBy,
    requestedAt: wire.requestedAt,
    suggestions: wire.suggestions.map((suggestion) => suggestionOf(suggestion, vocabulary)),
    rejectedProposals: wire.rejectedProposals.map((rejected) => rejectedProposalOf(rejected, vocabulary)),
  };
}
