import { useState } from 'react';
import type { AssistantReview, Suggestion } from '../../domain/assistance';
import { useCertiflow } from '../app/CertiflowContext';
import { type Query, useQuery } from '../shared/useQuery';

export interface AssistantReviews {
  query: Query<AssistantReview[]>;
  /** La revisión más reciente: es la que se muestra y la que marca los criterios. */
  latest?: AssistantReview;
  recordChange(): void;
}

/** Las revisiones de una inspección, compartidas por el panel y por las tarjetas de cada criterio. */
export function useAssistantReviews(inspectionId: string): AssistantReviews {
  const certiflow = useCertiflow();
  const [changeCount, setChangeCount] = useState(0);
  const query = useQuery(() => certiflow.assistance.listReviews(inspectionId), [inspectionId, changeCount]);
  const reviews = query.data ?? [];
  return {
    query,
    ...(reviews.length === 0 ? {} : { latest: reviews[reviews.length - 1] }),
    recordChange: () => setChangeCount((count) => count + 1),
  };
}

export function suggestionFor(review: AssistantReview | undefined, criterionCode: string): Suggestion | undefined {
  return review?.suggestions.find((suggestion) => suggestion.criterionCode === criterionCode);
}

export function pendingSuggestions(review: AssistantReview | undefined): Suggestion[] {
  return review?.suggestions.filter((suggestion) => suggestion.decision.status === 'PENDING') ?? [];
}
