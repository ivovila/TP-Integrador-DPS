import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { assetResponsible, verifier, yesterday, inEighteenDays, no } from './fixture';

import type { Person } from '../../domain/shared';


export function correctiveActionsContract(context: ContractContext): void {
    describe('corrective actions', () => {
      async function findingForBrokenSeal(): Promise<string> {
        const { findingIds } = await context.assetWithClosedInspection({ SEAL: no });
        return findingIds[0]!;
      }

      function verify(findingId: string, actionId: number, result: 'ACCEPTED' | 'REJECTED', notes = '', by: Person = verifier) {
        return context.certiflow.findings.verifyCorrectiveAction({ findingId, actionId, verifier: by, result, notes });
      }

      test('a corrective action is planned with its responsible, due date and who planned it', async () => {
        const findingId = await findingForBrokenSeal();
        const actionId = await context.planAction(findingId);
        const finding = await context.certiflow.findings.findFinding(findingId);
        expect(actionId).toBe(1);
        expect(finding.correctiveActions).toMatchObject([
          { id: 1, responsible: assetResponsible, dueDate: inEighteenDays, plannedBy: assetResponsible, state: 'OPEN', overdue: false },
        ]);
      });

      test('the due date cannot be before the day the action is planned', async () => {
        const findingId = await findingForBrokenSeal();
        await context.expectRejection(context.planAction(findingId, yesterday), 'DueDateInThePastException');
      });

      test('the responsible for an action cannot verify it', async () => {
        const findingId = await findingForBrokenSeal();
        await context.planAction(findingId, inEighteenDays, verifier);
        await context.expectRejection(
          verify(findingId, 1, 'ACCEPTED', '', verifier),
          'VerifierMustDifferFromResponsibleException',
        );
      });

      test('a rejected verification must explain why and leaves the action open', async () => {
        const findingId = await findingForBrokenSeal();
        await context.planAction(findingId);
        await context.expectRejection(verify(findingId, 1, 'REJECTED', ''), 'VerificationNotesRequiredException');
        await verify(findingId, 1, 'REJECTED', 'The seal is still missing');
        const finding = await context.certiflow.findings.findFinding(findingId);
        expect(finding.open).toBe(true);
        expect(finding.correctiveActions[0]).toMatchObject({
          state: 'OPEN',
          verifications: [{ result: 'REJECTED', notes: 'The seal is still missing', verifier }],
        });
      });

      test('an accepted verification closes the action and the finding and cancels the other open actions', async () => {
        const findingId = await findingForBrokenSeal();
        await context.planAction(findingId);
        await context.planAction(findingId);
        await verify(findingId, 2, 'ACCEPTED');
        const finding = await context.certiflow.findings.findFinding(findingId);
        const history = await context.certiflow.audit.auditHistory({ kind: 'finding', id: findingId });
        expect(finding.open).toBe(false);
        expect(finding.correctiveActions.map((action) => action.state)).toEqual(['CANCELLED', 'CLOSED']);
        expect(history.map((entry) => entry.action)).toEqual([
          'RAISED',
          'ACTION_PLANNED',
          'ACTION_PLANNED',
          'VERIFICATION_ACCEPTED',
        ]);
      });

      test('a closed finding accepts no more corrective actions or verifications', async () => {
        const findingId = await findingForBrokenSeal();
        await context.planAction(findingId);
        await verify(findingId, 1, 'ACCEPTED');
        await context.expectRejection(context.planAction(findingId), 'FindingAlreadyClosedException');
        await context.expectRejection(verify(findingId, 1, 'ACCEPTED'), 'FindingAlreadyClosedException');
      });

      test('a verification must point to an action of the finding', async () => {
        const findingId = await findingForBrokenSeal();
        await context.expectRejection(verify(findingId, 7, 'ACCEPTED'), 'CorrectiveActionNotInFindingException');
      });
    });

}
