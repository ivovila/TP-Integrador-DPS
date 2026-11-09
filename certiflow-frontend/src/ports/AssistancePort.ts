import type { AssistantReview } from '../domain/assistance';
import type { Person } from '../domain/shared';

export interface RequestAssistantReviewCommand {
  inspectionId: string;
  requestedBy: Person;
}

export interface AcceptSuggestionCommand {
  reviewId: string;
  position: number;
  acceptedBy: Person;
}

export interface DismissSuggestionCommand {
  reviewId: string;
  position: number;
  reason: string;
  dismissedBy: Person;
}

export interface AssistancePort {
  requestReview(command: RequestAssistantReviewCommand): Promise<AssistantReview>;
  /** Las revisiones de una inspección, en el orden en que se pidieron. */
  listReviews(inspectionId: string): Promise<AssistantReview[]>;
  acceptSuggestion(command: AcceptSuggestionCommand): Promise<void>;
  dismissSuggestion(command: DismissSuggestionCommand): Promise<void>;
}
