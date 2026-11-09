import { expect, test } from 'vitest';
import decidedReview from './__fixtures__/assistant-review-decided.json';
import pendingReview from './__fixtures__/assistant-review-pending.json';
import reviewsOfAnInspection from './__fixtures__/assistant-reviews.json';
import vocabularyFixture from './__fixtures__/vocabulary.json';
import { assistantReviewOf } from './assistanceMapping';
import { vocabularyOf } from './schemaMapping';
import type { WireAssistantReview, WireVocabulary } from './wire';

// Respuestas reales del backend con el stub: dos sugerencias y una propuesta que frenó un guardrail.
const vocabulary = vocabularyOf(vocabularyFixture as unknown as WireVocabulary);
const reviewOf = (fixture: unknown) => assistantReviewOf(fixture as WireAssistantReview, vocabulary);

test('a review keeps who requested it, the prompt version and the model that answered', () => {
  expect(reviewOf(pendingReview)).toMatchObject({
    id: pendingReview.id,
    inspectionId: pendingReview.inspectionId,
    promptVersion: 'stub',
    modelId: 'keyword-stub',
    requestedBy: { id: 'INSPECTOR' },
    requestedAt: pendingReview.requestedAt,
  });
});

test('a suggestion shows the severity of the criterion next to the one the assistant estimated, with its quotes', () => {
  expect(reviewOf(pendingReview).suggestions).toMatchObject([
    {
      position: 1,
      criterionCode: 'EXT-01',
      criterionSeverity: { name: 'MAJOR' },
      estimatedSeverity: { name: 'MAJOR' },
      quotes: ['Pérdida de presión en la válvula'],
    },
    { position: 2, criterionCode: 'EXT-02', criterionSeverity: { name: 'CRITICAL' }, estimatedSeverity: { name: 'MAJOR' } },
  ]);
});

test('a pending suggestion can be accepted or dismissed, and a decided one offers nothing', () => {
  expect(reviewOf(pendingReview).suggestions.map((suggestion) => [suggestion.decision, suggestion.actions])).toEqual([
    [{ status: 'PENDING' }, ['acceptSuggestion', 'dismissSuggestion']],
    [{ status: 'PENDING' }, ['acceptSuggestion', 'dismissSuggestion']],
  ]);
  expect(reviewOf(decidedReview).suggestions.map((suggestion) => suggestion.actions)).toEqual([[], []]);
});

test('a decision keeps who took it and when, and a dismissal its reason', () => {
  const [accepted, dismissed] = reviewOf(decidedReview).suggestions;
  expect(accepted!.decision).toEqual({
    status: 'ACCEPTED',
    decidedBy: { id: 'INSPECTOR', name: 'Person INSPECTOR' },
    decidedAt: decidedReview.suggestions[0]!.decision.decidedAt,
  });
  expect(dismissed!.decision).toMatchObject({ status: 'DISMISSED', reason: 'El precinto se cortó para la recarga' });
});

test('a proposal stopped by a guardrail is kept with the reason, and is never a suggestion', () => {
  const review = reviewOf(pendingReview);
  expect(review.rejectedProposals).toEqual([
    {
      criterionCode: 'EXT-03',
      estimatedSeverity: expect.objectContaining({ name: 'MAJOR' }),
      rationale: 'The inspector wrote something that names a failure',
      quotes: ['Una caja tapa el cartel'],
      reason: 'ALREADY_NONCONFORMING',
    },
  ]);
  expect(review.suggestions.map((suggestion) => suggestion.criterionCode)).not.toContain('EXT-03');
});

test('the reviews of an inspection come embedded as assistant-reviews', () => {
  expect(reviewsOfAnInspection._embedded['assistant-reviews'].map(reviewOf)).toMatchObject([{ id: decidedReview.id }]);
});
