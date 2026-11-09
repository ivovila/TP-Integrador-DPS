import { describe, expect, test } from 'vitest';
import type { ContractContext } from './fixture';
import { lastDayOfAYear, lastDayOfSixMonths, lastDayOfThirtyDays, no, measured } from './fixture';
import type { RawAnswer } from '../../domain/schema';



export function jurisdictionPoliciesContract(context: ContractContext): void {
    describe('jurisdiction policies', () => {
      const decisionsByJurisdiction: [string, Record<string, RawAnswer>, string, string][] = [
        ['DEMO-STRICT', {}, 'APPROVED', lastDayOfAYear],
        ['DEMO-STANDARD', {}, 'APPROVED', lastDayOfAYear],
        ['DEMO-CONDITIONAL', {}, 'APPROVED', lastDayOfSixMonths],
        ['DEMO-STRICT', { SIGNAGE: no }, 'REJECTED', ''],
        ['DEMO-STANDARD', { SIGNAGE: no }, 'APPROVED', lastDayOfAYear],
        ['DEMO-CONDITIONAL', { SIGNAGE: no }, 'CONDITIONAL', lastDayOfThirtyDays],
        ['DEMO-STRICT', { SEAL: no }, 'REJECTED', ''],
        ['DEMO-STANDARD', { SEAL: no }, 'REJECTED', ''],
        ['DEMO-CONDITIONAL', { SEAL: no }, 'CONDITIONAL', lastDayOfThirtyDays],
        ['DEMO-CONDITIONAL', { PRESSURE: measured('20') }, 'REJECTED', ''],
      ];

      test.each(decisionsByJurisdiction)(
        'under %s the answers %o give a %s decision valid until %s',
        async (jurisdictionCode, overrides, expectedOutcome, expectedValidUntil) => {
          await context.registerExtinguisher(context.extinguisherCode, jurisdictionCode);
          await context.createExtinguisherSchema();
          const { inspectionId } = await context.closedInspection(overrides);
          const decision = await context.certiflow.certification.evaluateCertification(inspectionId);
          expect(decision).toMatchObject({ outcome: expectedOutcome, jurisdictionCode, scope: { kind: 'wholeAsset' } });
          expect(decision.validity?.validUntil ?? '').toBe(expectedValidUntil);
        },
      );

      test('a conditional certificate lists every open finding as a condition due when it expires', async () => {
        await context.registerExtinguisher(context.extinguisherCode, 'DEMO-CONDITIONAL');
        await context.createExtinguisherSchema();
        const { inspectionId, findingIds } = await context.closedInspection({ SEAL: no });
        const number = await context.issue(inspectionId);
        const certificate = await context.certiflow.certification.findCertificate(number);
        expect(certificate).toMatchObject({ modality: 'CONDITIONAL', status: 'ACTIVE', validUntil: lastDayOfThirtyDays });
        expect(certificate.decision.conditions).toMatchObject([
          { findingId: findingIds[0], criterionCode: 'SEAL', dueDate: lastDayOfThirtyDays },
        ]);
      });

      test('a rejected decision names the policy that rejected it and why', async () => {
        await context.registerExtinguisher(context.extinguisherCode, 'DEMO-STRICT');
        await context.createExtinguisherSchema();
        const { inspectionId } = await context.closedInspection({ SIGNAGE: no });
        await expect(context.issue(inspectionId)).rejects.toMatchObject({
          name: 'NotEligibleForCertificationException',
          ineligibilities: [{ reason: 'BLOCKING_FINDINGS' }],
        });
        expect(await context.certiflow.certification.evaluateCertification(inspectionId)).toMatchObject({
          policyCode: 'STRICT',
          actions: [],
        });
      });

      test('an asset cannot be registered under a jurisdiction without a policy', async () => {
        await context.expectRejection(context.registerExtinguisher(context.extinguisherCode, 'ATLANTIS'), 'UnknownJurisdictionException');
      });

      test('the vocabulary lists the jurisdictions that have a policy', async () => {
        expect(context.vocabulary.jurisdictions.map((jurisdiction) => jurisdiction.code)).toEqual(
          expect.arrayContaining(['DEMO-STRICT', 'DEMO-CONDITIONAL', 'DEMO-STANDARD']),
        );
      });
    });

}
