import type { AssistancePort } from '../../ports/AssistancePort';
import { assistantReviewOf } from './assistanceMapping';
import { type HalClient, segment } from './HalClient';
import type { SchemaReader } from './HttpSchemaPort';
import type { WireAssistantReview } from './wire';

function reviewsOfInspection(inspectionId: string): string {
  return `/api/v1/inspections/${segment(inspectionId)}/assistant-reviews`;
}

function suggestionPath(reviewId: string, position: number): string {
  return `/api/v1/assistant-reviews/${segment(reviewId)}/suggestions/${segment(position)}`;
}

export function createHttpAssistancePort(client: HalClient, schemas: SchemaReader): AssistancePort {
  return {
    async requestReview(command) {
      const [review, vocabulary] = await Promise.all([
        client.write<WireAssistantReview>('POST', reviewsOfInspection(command.inspectionId), command.requestedBy),
        schemas.vocabulary(),
      ]);
      return assistantReviewOf(review, vocabulary);
    },

    async listReviews(inspectionId) {
      const [reviews, vocabulary] = await Promise.all([
        client.readAll<WireAssistantReview>(reviewsOfInspection(inspectionId), 'assistant-reviews'),
        schemas.vocabulary(),
      ]);
      return reviews.map((review) => assistantReviewOf(review, vocabulary));
    },

    async acceptSuggestion(command) {
      await client.write('POST', `${suggestionPath(command.reviewId, command.position)}/acceptance`, command.acceptedBy);
    },

    async dismissSuggestion(command) {
      await client.write('POST', `${suggestionPath(command.reviewId, command.position)}/dismissal`, command.dismissedBy, {
        reason: command.reason,
      });
    },
  };
}
