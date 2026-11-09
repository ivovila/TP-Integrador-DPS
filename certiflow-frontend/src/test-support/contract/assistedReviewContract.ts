import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { inspector, chosen } from './fixture';




export function assistedReviewContract(context: ContractContext): void {
    describe('assisted review', () => {
      // El backend de e2e corre con el stub, que propone un hallazgo donde el texto nombra una falla.
      async function inspectionWithACrackedHose(): Promise<string> {
        const inspectionId = await context.assetWithSchemaAndOpenInspection();
        await context.answer(inspectionId, 'HOSE', chosen('Good'));
        await context.certiflow.inspections.addObservation({
          inspectionId,
          criterionCode: 'HOSE',
          text: 'La manguera tiene una grieta cerca del acople',
          observedBy: inspector,
        });
        return inspectionId;
      }

      function requestReview(inspectionId: string) {
        return context.certiflow.assistance.requestReview({ inspectionId, requestedBy: inspector });
      }

      test('the assistant suggests a non-conformity that the free text describes and the answer does not reflect', async () => {
        const inspectionId = await inspectionWithACrackedHose();
        const review = await requestReview(inspectionId);
        expect(review).toMatchObject({ inspectionId, requestedBy: inspector, promptVersion: 'stub', modelId: 'keyword-stub' });
        expect(review.suggestions).toMatchObject([
          {
            position: 1,
            criterionCode: 'HOSE',
            criterionSeverity: { name: 'MINOR' },
            quotes: ['La manguera tiene una grieta cerca del acople'],
            decision: { status: 'PENDING' },
            actions: ['acceptSuggestion', 'dismissSuggestion'],
          },
        ]);
        expect(await context.certiflow.assistance.listReviews(inspectionId)).toMatchObject([{ id: review.id }]);
      });

      test('accepting only records the decision: the inspection does not change', async () => {
        const inspectionId = await inspectionWithACrackedHose();
        const review = await requestReview(inspectionId);
        await context.certiflow.assistance.acceptSuggestion({ reviewId: review.id, position: 1, acceptedBy: inspector });
        const [decided] = await context.certiflow.assistance.listReviews(inspectionId);
        expect(decided!.suggestions[0]).toMatchObject({ decision: { status: 'ACCEPTED', decidedBy: inspector }, actions: [] });
        expect(await context.outcomeOf(inspectionId, 'HOSE')).toBe('APPROVED');
        await context.expectRejection(
          context.certiflow.assistance.dismissSuggestion({ reviewId: review.id, position: 1, reason: 'Changed my mind', dismissedBy: inspector }),
          'SuggestionAlreadyDecidedException',
        );
      });

      test('dismissing needs a reason, which stays with the decision', async () => {
        const inspectionId = await inspectionWithACrackedHose();
        const review = await requestReview(inspectionId);
        const dismiss = (reason: string) =>
          context.certiflow.assistance.dismissSuggestion({ reviewId: review.id, position: 1, reason, dismissedBy: inspector });
        await context.expectRejection(dismiss(' '), 'DismissalReasonRequiredException');
        await dismiss('The crack is on the hose that was replaced');
        const [decided] = await context.certiflow.assistance.listReviews(inspectionId);
        expect(decided!.suggestions[0]!.decision).toMatchObject({ status: 'DISMISSED', reason: 'The crack is on the hose that was replaced' });
      });

      test('a closed inspection cannot be reviewed, but its reviews can still be read', async () => {
        const inspectionId = await inspectionWithACrackedHose();
        await requestReview(inspectionId);
        await context.answerEverything(inspectionId);
        await context.close(inspectionId);
        await context.expectRejection(requestReview(inspectionId), 'ReviewOfClosedInspectionException');
        expect(await context.certiflow.assistance.listReviews(inspectionId)).toHaveLength(1);
      });
    });

}
